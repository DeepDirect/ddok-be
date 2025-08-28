// goorm/ddok/study/service/StudyRecruitmentEditService.java
package goorm.ddok.study.service;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.file.FileService;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.global.util.BannerImageService;
import goorm.ddok.member.domain.User;
import goorm.ddok.study.domain.*;
import goorm.ddok.study.dto.request.StudyRecruitmentUpdateRequest;
import goorm.ddok.study.dto.response.StudyEditPageResponse;
import goorm.ddok.study.dto.response.StudyUpdateResultResponse;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import goorm.ddok.study.repository.StudyRecruitmentTraitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyRecruitmentEditService {

    private final StudyRecruitmentRepository recruitmentRepository;
    private final StudyRecruitmentTraitRepository traitRepository;
    private final StudyApplicationRepository applicationRepository;

    private final FileService fileService;
    private final BannerImageService bannerImageService;

    /* =========================
     *  수정 페이지 조회
     * ========================= */
    @Transactional(readOnly = true)
    public StudyEditPageResponse getEditPage(Long studyId, CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        StudyRecruitment sr = recruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));

        // 리더만 조회
        if (!Objects.equals(sr.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        long applicantCount = applicationRepository.countByStudyRecruitment_Id(studyId);

        String address = (sr.getMode() == StudyMode.ONLINE)
                ? "ONLINE"
                : composeAddressFromEntity(sr);

        PreferredAgesDto ages = (isAgeUnlimited(sr.getAgeMin(), sr.getAgeMax()))
                ? null
                : new PreferredAgesDto(sr.getAgeMin(), sr.getAgeMax());

        return StudyEditPageResponse.builder()
                .title(sr.getTitle())
                .teamStatus(sr.getTeamStatus().name())
                .bannerImageUrl(sr.getBannerImageUrl())
                .traits(sr.getTraits().stream().map(StudyRecruitmentTrait::getTraitName).toList())
                .capacity(sr.getCapacity())
                .applicantCount(applicantCount)
                .mode(sr.getMode().name().toLowerCase())
                .address(address)
                .preferredAges(ages)
                .expectedMonth(sr.getExpectedMonths())
                .startDate(sr.getStartDate())
                .detail(sr.getContentMd())
                .build();
    }

    /* =========================
     *  수정 저장 (업데이트 방식)
     * ========================= */
    public StudyUpdateResultResponse updateStudy(Long studyId,
                                                 StudyRecruitmentUpdateRequest req,
                                                 MultipartFile bannerImage,
                                                 CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        StudyRecruitment sr = recruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));

        // 리더만 수정
        if (!Objects.equals(sr.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        // 과거 시작일 금지
        if (req.getExpectedStart() != null && req.getExpectedStart().isBefore(LocalDate.now())) {
            throw new GlobalException(ErrorCode.INVALID_START_DATE);
        }

        // OFFLINE 검증
        if (req.getMode() == StudyMode.OFFLINE) {
            LocationDto loc = req.getLocation();
            if (loc == null || loc.getLatitude() == null || loc.getLongitude() == null) {
                throw new GlobalException(ErrorCode.INVALID_LOCATION);
            }
        }

        // 연령 10단위/무관 검증
        int ageMin;
        int ageMax;
        if (req.getPreferredAges() == null) {
            ageMin = 0; ageMax = 0;
        } else {
            ageMin = req.getPreferredAges().getAgeMin();
            ageMax = req.getPreferredAges().getAgeMax();
            if (ageMin > ageMax) throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
            if (ageMin % 10 != 0 || ageMax % 10 != 0) throw new GlobalException(ErrorCode.INVALID_AGE_BUCKET);
        }

        // 배너 URL 결정: 파일 > request.bannerImageUrl > 기존 > 기본생성
        String bannerUrl = resolveBannerUrl(bannerImage, req.getBannerImageUrl(), sr.getBannerImageUrl(), req.getTitle());

        // 위치 업데이트
        boolean offline = req.getMode() == StudyMode.OFFLINE;
        if (offline) {
            sr = updateOfflineLocation(sr, req.getLocation());
        } else {
            sr = clearLocation(sr);
        }

        // 기본 필드 업데이트
        sr = sr.toBuilder()
                .title(req.getTitle())
                .teamStatus(req.getTeamStatus())
                .startDate(req.getExpectedStart())
                .expectedMonths(req.getExpectedMonth())
                .mode(req.getMode())
                .bannerImageUrl(bannerUrl)
                .studyType(req.getStudyType())
                .contentMd(req.getDetail())
                .capacity(req.getCapacity())
                .ageMin(ageMin)
                .ageMax(ageMax)
                .build();

        // 성향 머지 (완전 치환 전략: 요청에 없는 건 삭제)
        mergeTraitsReplace(sr, req.getTraits());

        StudyRecruitment saved = recruitmentRepository.save(sr);

        return buildUpdateResult(saved, me);
    }

    /* ---------- traits replace (요청 목록으로 완전 치환) ---------- */
    private void mergeTraitsReplace(StudyRecruitment sr, List<String> incoming) {
        List<String> desired = (incoming == null) ? List.of()
                : incoming.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        // 기존 모두 제거 후
        sr.getTraits().clear();

        // 새로 추가
        for (String name : desired) {
            sr.getTraits().add(StudyRecruitmentTrait.builder()
                    .studyRecruitment(sr)
                    .traitName(name)
                    .build());
        }
    }

    /* ---------- location helpers ---------- */

    /** 요청(LocationDto) → 엔티티 필드 업데이트 */
    private StudyRecruitment updateOfflineLocation(StudyRecruitment sr, LocationDto loc) {
        BigDecimal lat = loc.getLatitude();
        BigDecimal lng = loc.getLongitude();

        // 개별 필드 저장 (roadName만 저장하지 않고, region1/2/3 + roadName를 각각 유지)
        return sr.toBuilder()
                .region1DepthName(nullToEmpty(loc.getRegion1depthName()))
                .region2DepthName(nullToEmpty(loc.getRegion2depthName()))
                .region3DepthName(nullToEmpty(loc.getRegion3depthName()))
                .roadName(nullToEmpty(loc.getRoadName()))
                .latitude(lat)
                .longitude(lng)
                .build();
    }

    private StudyRecruitment clearLocation(StudyRecruitment sr) {
        return sr.toBuilder()
                .region1DepthName(null)
                .region2DepthName(null)
                .region3DepthName(null)
                .roadName(null)
                .latitude(null)
                .longitude(null)
                .build();
    }

    /** 응답용: 요청(LocationDto)에서 주소 문자열 합성(요청 검증에 사용할 때) */
    private String composeAddress(LocationDto loc) {
        // 클라이언트가 address 전체 문자열을 줬다면 그걸 최우선 사용
        if (loc.getAddress() != null && !loc.getAddress().isBlank()) {
            return loc.getAddress().trim();
        }
        // 아니면 구성 요소를 합쳐서 생성
        StringBuilder sb = new StringBuilder();
        appendToken(sb, loc.getRegion1depthName());
        appendToken(sb, loc.getRegion2depthName());
        appendToken(sb, loc.getRegion3depthName());
        appendToken(sb, loc.getRoadName());
        return sb.length() == 0 ? "-" : sb.toString();
    }

    /** 응답용: 엔티티 필드에서 주소 문자열 합성 */
    private String composeAddressFromEntity(StudyRecruitment sr) {
        StringBuilder sb = new StringBuilder();
        appendToken(sb, sr.getRegion1DepthName());
        appendToken(sb, sr.getRegion2DepthName());
        appendToken(sb, sr.getRegion3DepthName());
        appendToken(sb, sr.getRoadName());
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private void appendToken(StringBuilder sb, String token) {
        if (token != null && !token.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(token.trim());
        }
    }

    private String nullToEmpty(String s) {
        return (s == null) ? "" : s.trim();
    }

    /* ---------- banner helper ---------- */
    private String resolveBannerUrl(MultipartFile file, String requestUrl, String currentUrl, String titleForDefault) {
        if (file != null && !file.isEmpty()) {
            try {
                return fileService.upload(file);
            } catch (IOException e) {
                throw new GlobalException(ErrorCode.BANNER_UPLOAD_FAILED);
            }
        }
        if (requestUrl != null && !requestUrl.isBlank()) return requestUrl.trim();
        if (currentUrl != null && !currentUrl.isBlank()) return currentUrl;
        return bannerImageService.generateBannerImageUrl(
                (titleForDefault == null ? "STUDY" : titleForDefault), "STUDY", 1200, 600
        );
    }

    /* ---------- response builders ---------- */

    private StudyUpdateResultResponse buildUpdateResult(StudyRecruitment sr, CustomUserDetails me) {
        Long studyId = sr.getId();
        long applicantCount = applicationRepository.countByStudyRecruitment_Id(studyId);

        String address = (sr.getMode() == StudyMode.ONLINE)
                ? "ONLINE"
                : composeAddressFromEntity(sr);

        boolean isMine = me != null && me.getUser() != null
                && Objects.equals(sr.getUser().getId(), me.getUser().getId());

        PreferredAgesDto ages =
                isAgeUnlimited(sr.getAgeMin(), sr.getAgeMax())
                        ? null
                        : new PreferredAgesDto(sr.getAgeMin(), sr.getAgeMax());

        return StudyUpdateResultResponse.builder()
                .studyId(studyId)
                .IsMine(isMine)
                .title(sr.getTitle())
                .teamStatus(sr.getTeamStatus().name())
                .bannerImageUrl(sr.getBannerImageUrl())
                .traits(sr.getTraits().stream().map(StudyRecruitmentTrait::getTraitName).toList())
                .capacity(sr.getCapacity())
                .applicantCount(applicantCount)
                .mode(sr.getMode().name().toLowerCase())
                .address(address)
                .preferredAges(ages)
                .expectedMonth(sr.getExpectedMonths())
                .startDate(sr.getStartDate())
                .studyType(sr.getStudyType().name())
                .detail(sr.getContentMd())
                .build();
    }

    private boolean isAgeUnlimited(Integer min, Integer max) {
        // null-safe: 엔티티는 primitive가 아니므로 null 가능성 고려
        return (min == null || min == 0) && (max == null || max == 0);
    }
}