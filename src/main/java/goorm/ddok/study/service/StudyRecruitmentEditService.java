package goorm.ddok.study.service;

import goorm.ddok.badge.service.BadgeService;
import goorm.ddok.chat.service.ChatRoomService;
import goorm.ddok.chat.service.DmRequestCommandService;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.file.FileService;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.global.util.BannerImageService;
import goorm.ddok.study.domain.*;
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

import java.io.IOException;
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
    private final BannerImageService bannerImageService;
    private final FileService fileService;
    private final BadgeService badgeService;
    private final ChatRoomService chatRoomService;
    private final DmRequestCommandService dmRequestService;


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

        if (req.getCapacity() == null || req.getCapacity() < 1 || req.getCapacity() > 7) {
            throw new GlobalException(ErrorCode.INVALID_CAPACITY);
        }

        // 위치 검증
        if (req.getMode() == StudyMode.offline) {
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
            if (ageMin % 10 != 0 || ageMax % 10 != 0) throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
        }

        // 위치 반영
        if (req.getMode() == StudyMode.offline) {
            study = applyOfflineLocation(study, req);
        } else if (req.getMode() == StudyMode.online) {
            study = clearLocation(study);
        }

        String bannerUrl;
        if (req.getBannerImageUrl() != null){
            bannerUrl = req.getBannerImageUrl();
        } else if (bannerImage != null && !bannerImage.isEmpty()){
            bannerUrl = uploadBannerImage(bannerImage);
        } else {
            bannerUrl = bannerImageService.generateBannerImageUrl(req.getTitle(), "STUDY", 1200, 600);
        }


        // 기본 필드 반영 (배너 업로드는 생략: 요청값 없으면 기존 유지)
        study = study.toBuilder()
                .title(Optional.ofNullable(req.getTitle()).orElse(study.getTitle()))
                .teamStatus(Optional.ofNullable(req.getTeamStatus()).orElse(study.getTeamStatus()))
                .startDate(Optional.ofNullable(req.getExpectedStart()).orElse(study.getStartDate()))
                .expectedMonths(Optional.ofNullable(req.getExpectedMonth()).orElse(study.getExpectedMonths()))
                .mode(Optional.ofNullable(req.getMode()).orElse(study.getMode()))
                .bannerImageUrl(bannerUrl)
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

        int applicantCount = (int) studyApplicationRepository
                .countByStudyRecruitmentAndApplicationStatusNot(study, ApplicationStatus.REJECTED);
        int participantsCount = members.size();

        UserSummaryDto leaderDto = toUserSummaryDto(leader, me);
        List<UserSummaryDto> participantDtos = members.stream()
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getUser().getNickname()).orElse("")))
                .map(p -> toUserSummaryDto(p, me))
                .toList();

        PreferredAgesDto ages = PreferredAgesDto.of(
                study.getAgeMin(),
                study.getAgeMax()
        );

        LocationDto location = buildLocationForRead(study);

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
                .location(location)
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
                .region1depthName(req.getLocation().getRegion1depthName())
                .region2depthName(req.getLocation().getRegion2depthName())
                .region3depthName(req.getLocation().getRegion3depthName())
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
                .region1depthName(null)
                .region2depthName(null)
                .region3depthName(null)
                .roadName(null)
                .mainBuildingNo(null)
                .subBuildingNo(null)
                .zoneNo(null)
                .latitude(null)
                .longitude(null)
                .build();
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

        BadgeDto mainBadge =  badgeService.getRepresentativeGoodBadge(u);
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(u);


        Long chatRoomId = null;
        boolean dmPending = false;

        if (me != null && !Objects.equals(me.getId(), u.getId())) {
            Long meId = me.getId();
            Long otherId = u.getId();

            chatRoomId = chatRoomService.findPrivateRoomId(meId, otherId).orElse(null);
            dmPending = (chatRoomId != null)
                    || dmRequestService.isDmPendingOrAcceptedOrChatExists(meId, otherId);
        }

        return UserSummaryDto.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .mainPosition(mainPosition)
                .mainBadge(mainBadge)           // null
                .abandonBadge(abandonBadge)     // null
                .temperature(temperature)       // null 허용
                .IsMine(me != null && Objects.equals(me.getId(), u.getId()))
                .chatRoomId(chatRoomId)
                .dmRequestPending(dmPending)
                .build();
    }
    private LocationDto buildLocationForRead(StudyRecruitment sr) {
        if (sr.getMode() == StudyMode.online) return null;
        String full = composeFullAddress(
                sr.getRegion1depthName(), sr.getRegion2depthName(), sr.getRegion3depthName(),
                sr.getRoadName(), sr.getMainBuildingNo(), sr.getSubBuildingNo()
        );
        return LocationDto.builder()
                .address(full)
                .region1depthName(sr.getRegion1depthName())
                .region2depthName(sr.getRegion2depthName())
                .region3depthName(sr.getRegion3depthName())
                .roadName(sr.getRoadName())
                .mainBuildingNo(sr.getMainBuildingNo())
                .subBuildingNo(sr.getSubBuildingNo())
                .zoneNo(sr.getZoneNo())
                .latitude(sr.getLatitude())
                .longitude(sr.getLongitude())
                .build();
    }

    private String composeFullAddress(String r1, String r2, String r3, String road, String main, String sub) {
        StringBuilder sb = new StringBuilder();
        if (r1 != null && !r1.isBlank()) sb.append(r1).append(" ");
        if (r2 != null && !r2.isBlank()) sb.append(r2).append(" ");
        if (r3 != null && !r3.isBlank()) sb.append(r3).append(" ");
        if (road != null && !road.isBlank()) sb.append(road).append(" ");
        if (main != null && !main.isBlank()) {
            sb.append(main);
            if (sub != null && !sub.isBlank()) sb.append("-").append(sub);
        }
        String s = sb.toString().trim().replaceAll("\\s+", " ");
        return s.isBlank() ? null : s;
    }

    private String uploadBannerImage(MultipartFile file) {
        try {
            return fileService.upload(file);
        } catch (IOException e) {
            throw new GlobalException(ErrorCode.BANNER_UPLOAD_FAILED);
        }
    }
}