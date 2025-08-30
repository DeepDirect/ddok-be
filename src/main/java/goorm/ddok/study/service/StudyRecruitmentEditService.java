package goorm.ddok.study.service;

import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.domain.ParticipantRole;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.domain.StudyRecruitmentTrait;
import goorm.ddok.study.dto.UserSummaryDto;
import goorm.ddok.study.dto.request.StudyRecruitmentUpdateRequest;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyRecruitmentEditService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final StudyApplicationRepository studyApplicationRepository;
    private final UserReputationRepository userReputationRepository;

    /* =========================
     * 수정 페이지 조회 (상세 스키마 동일)
     * ========================= */
    @Transactional(readOnly = true)
    public StudyRecruitmentDetailResponse getEditPage(Long studyId, CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));
        if (study.getDeletedAt() != null) throw new GlobalException(ErrorCode.STUDY_NOT_FOUND);

        if (!Objects.equals(study.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        return buildDetailResponse(study, me.getUser());
    }

    /* =========================
     * 수정 저장 (상세 스키마 동일)
     * ========================= */
    public StudyRecruitmentDetailResponse updateStudy(Long studyId,
                                                      StudyRecruitmentUpdateRequest req,
                                                      MultipartFile bannerImage, // 컨트롤러 시그니처 호환용
                                                      CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));
        if (study.getDeletedAt() != null) throw new GlobalException(ErrorCode.STUDY_NOT_FOUND);

        if (!Objects.equals(study.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        // 과거 시작일 금지
        if (req.getExpectedStart() != null && req.getExpectedStart().isBefore(LocalDate.now())) {
            throw new GlobalException(ErrorCode.INVALID_START_DATE);
        }

        // 위치 검증
        if (req.getMode() == StudyMode.OFFLINE) {
            if (req.getLocation() == null
                    || req.getLocation().getLatitude() == null
                    || req.getLocation().getLongitude() == null) {
                throw new GlobalException(ErrorCode.INVALID_LOCATION);
            }
        }

        // 연령 검증 (무관: null 또는 0/0 → 저장은 0/0)
        int ageMin = 0, ageMax = 0;
        if (req.getPreferredAges() != null) {
            ageMin = Optional.ofNullable(req.getPreferredAges().getAgeMin()).orElse(0);
            ageMax = Optional.ofNullable(req.getPreferredAges().getAgeMax()).orElse(0);
            if (!(ageMin == 0 && ageMax == 0) && ageMin > ageMax) {
                throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
            }
        }

        // 위치 반영
        if (req.getMode() == StudyMode.OFFLINE) {
            study = applyOfflineLocation(study, req);
        } else if (req.getMode() == StudyMode.ONLINE) {
            study = clearLocation(study);
        }

        // 기본 필드 반영 (배너 업로드는 생략: 요청값 없으면 기존 유지)
        study = study.toBuilder()
                .title(Optional.ofNullable(req.getTitle()).orElse(study.getTitle()))
                .teamStatus(Optional.ofNullable(req.getTeamStatus()).orElse(study.getTeamStatus()))
                .startDate(Optional.ofNullable(req.getExpectedStart()).orElse(study.getStartDate()))
                .expectedMonths(Optional.ofNullable(req.getExpectedMonth()).orElse(study.getExpectedMonths()))
                .mode(Optional.ofNullable(req.getMode()).orElse(study.getMode()))
                .bannerImageUrl(Optional.ofNullable(req.getBannerImageUrl()).orElse(study.getBannerImageUrl()))
                .studyType(Optional.ofNullable(req.getStudyType()).orElse(study.getStudyType()))
                .contentMd(Optional.ofNullable(req.getDetail()).orElse(study.getContentMd()))
                .capacity(Optional.ofNullable(req.getCapacity()).orElse(study.getCapacity()))
                .ageMin(ageMin)
                .ageMax(ageMax)
                .build();

        // traits 머지 (요청 null이면 변경 없음)
        mergeTraits(study, req.getTraits());

        StudyRecruitment saved = studyRecruitmentRepository.save(study);
        return buildDetailResponse(saved, me.getUser());
    }

    /* =========================
     * 공통 응답 빌더
     * ========================= */
    private StudyRecruitmentDetailResponse buildDetailResponse(StudyRecruitment study, User me) {
        List<StudyParticipant> all = studyParticipantRepository.findByStudyRecruitment(study);

        StudyParticipant leader = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));

        List<StudyParticipant> members = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .toList();

        int applicantCount = (int) studyApplicationRepository.countByStudyRecruitment(study);
        int participantsCount = members.size();

        UserSummaryDto leaderDto = toUserSummaryDto(leader, me);
        List<UserSummaryDto> participantDtos = members.stream()
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getUser().getNickname()).orElse("")))
                .map(p -> toUserSummaryDto(p, me))
                .toList();

        PreferredAgesDto ages = (isZero(study.getAgeMin()) && isZero(study.getAgeMax()))
                ? null
                : new PreferredAgesDto(study.getAgeMin(), study.getAgeMax());

        return StudyRecruitmentDetailResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .studyType(study.getStudyType())
                .IsMine(me != null && Objects.equals(study.getUser().getId(), me.getId()))
                .teamStatus(study.getTeamStatus())
                .bannerImageUrl(study.getBannerImageUrl())
                .traits(study.getTraits().stream().map(StudyRecruitmentTrait::getTraitName).toList())
                .capacity(study.getCapacity())
                .applicantCount(applicantCount)
                .mode(study.getMode())
                .address(composeFullAddress(study))
                .preferredAges(ages)
                .expectedMonth(study.getExpectedMonths())
                .startDate(study.getStartDate())
                .detail(study.getContentMd())
                .leader(leaderDto)
                .participants(participantDtos)
                .participantsCount(participantsCount)
                .build();
    }

    /* ===== helpers ===== */

    private boolean isZero(Integer v) { return v == null || v == 0; }

    /** 오프라인 주소 저장 */
    private StudyRecruitment applyOfflineLocation(StudyRecruitment s, StudyRecruitmentUpdateRequest req) {
        return s.toBuilder()
                .region1DepthName(req.getLocation().getRegion1depthName())
                .region2DepthName(req.getLocation().getRegion2depthName())
                .region3DepthName(req.getLocation().getRegion3depthName())
                .roadName(req.getLocation().getRoadName())
                .mainBuildingNo(req.getLocation().getMainBuildingNo())
                .subBuildingNo(req.getLocation().getSubBuildingNo())
                .zoneNo(req.getLocation().getZoneNo())
                .latitude(req.getLocation().getLatitude())
                .longitude(req.getLocation().getLongitude())
                .build();
    }

    /** 온라인이면 위치 초기화 */
    private StudyRecruitment clearLocation(StudyRecruitment s) {
        return s.toBuilder()
                .region1DepthName(null)
                .region2DepthName(null)
                .region3DepthName(null)
                .roadName(null)
                .mainBuildingNo(null)
                .subBuildingNo(null)
                .zoneNo(null)
                .latitude(null)
                .longitude(null)
                .build();
    }

    /** 주소 합성 (ONLINE → null) */
    private String composeFullAddress(StudyRecruitment s) {
        if (s.getMode() == StudyMode.ONLINE) return null;

        String r1 = Optional.ofNullable(s.getRegion1DepthName()).orElse("");
        String r2 = Optional.ofNullable(s.getRegion2DepthName()).orElse("");
        String r3 = Optional.ofNullable(s.getRegion3DepthName()).orElse("");
        String road = Optional.ofNullable(s.getRoadName()).orElse("");
        String main = Optional.ofNullable(s.getMainBuildingNo()).orElse("");
        String sub  = Optional.ofNullable(s.getSubBuildingNo()).orElse("");

        StringBuilder sb = new StringBuilder();
        if (!r1.isBlank()) sb.append(r1).append(" ");
        if (!r2.isBlank()) sb.append(r2).append(" ");
        if (!r3.isBlank()) sb.append(r3).append(" ");
        if (!road.isBlank()) sb.append(road).append(" ");
        if (!main.isBlank() && !sub.isBlank()) sb.append(main).append("-").append(sub);
        else if (!main.isBlank()) sb.append(main);

        String addr = sb.toString().trim().replaceAll("\\s+", " ");
        return addr.isBlank() ? null : addr;
    }

    /** traits 병합 (요청 null → 변경 없음) */
    private void mergeTraits(StudyRecruitment study, List<String> incoming) {
        if (incoming == null) return;

        List<String> desired = incoming.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        Map<String, StudyRecruitmentTrait> current = study.getTraits().stream()
                .collect(Collectors.toMap(StudyRecruitmentTrait::getTraitName, t -> t, (a, b) -> a));

        for (String name : desired) {
            if (!current.containsKey(name)) {
                study.getTraits().add(StudyRecruitmentTrait.builder()
                        .studyRecruitment(study)
                        .traitName(name)
                        .build());
            }
        }
        study.getTraits().removeIf(t -> !desired.contains(t.getTraitName()));
    }

    /** 요약 변환: 온도/뱃지 “없으면 null” */
    private UserSummaryDto toUserSummaryDto(StudyParticipant participant, User me) {
        User u = participant.getUser();

        String mainPosition = u.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        BigDecimal temperature = userReputationRepository.findByUserId(u.getId())
                .map(UserReputation::getTemperature)
                .orElse(null); // null 허용

        BadgeDto mainBadge = null;
        AbandonBadgeDto abandonBadge = null;

        return UserSummaryDto.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .mainPosition(mainPosition)
                .mainBadge(mainBadge)           // null
                .abandonBadge(abandonBadge)     // null
                .temperature(temperature)       // null 허용
                .IsMine(me != null && Objects.equals(me.getId(), u.getId()))
                .chatRoomId(null)
                .dmRequestPending(false)
                .build();
    }
}