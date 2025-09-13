package goorm.ddok.study.service;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.file.FileService;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.global.util.BannerImageService;
import goorm.ddok.member.domain.User;
import goorm.ddok.notification.event.StudyJoinRequestedEvent;
import goorm.ddok.study.domain.*;
import goorm.ddok.study.dto.request.StudyRecruitmentCreateRequest;
import goorm.ddok.study.dto.response.StudyRecruitmentCreateResponse;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import goorm.ddok.team.domain.TeamType;
import goorm.ddok.team.service.TeamCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyRecruitmentService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final StudyParticipantRepository participantRepository;
    private final StudyApplicationRepository studyApplicationRepository;
    private final BannerImageService bannerImageService;
    private final FileService fileService;
    private final TeamCommandService teamCommandService;
    private final ApplicationEventPublisher eventPublisher;

    public StudyRecruitmentCreateResponse createStudy(StudyRecruitmentCreateRequest req,
                                                      MultipartFile bannerImage,
                                                      CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
        User user = userDetails.getUser();

        if (req.getExpectedStart().isBefore(LocalDate.now())) {
            throw new GlobalException(ErrorCode.INVALID_START_DATE);
        }

        if (req.getMode() == StudyMode.offline) {
            LocationDto loc = req.getLocation();
            if (loc == null || loc.getLatitude() == null || loc.getLongitude() == null) {
                throw new GlobalException(ErrorCode.INVALID_LOCATION);
            }
        }

        // 연령대 범위 검증
        validatePreferredAges(req.getPreferredAges());

        Integer ageMin = (req.getPreferredAges() != null) ? req.getPreferredAges().getAgeMin() : null;
        Integer ageMax = (req.getPreferredAges() != null) ? req.getPreferredAges().getAgeMax() : null;


      // 배너 이미지 업로드 or 기본값
        String bannerUrl = (bannerImage != null && !bannerImage.isEmpty())
                ? uploadBannerImage(bannerImage)
                : bannerImageService.generateBannerImageUrl(req.getTitle(), "STUDY", 1200, 600);

        StudyRecruitment.StudyRecruitmentBuilder b = StudyRecruitment.builder()
                .user(user)
                .title(req.getTitle())
                .teamStatus(TeamStatus.RECRUITING)
                .startDate(req.getExpectedStart())
                .expectedMonths(req.getExpectedMonth())
                .mode(req.getMode())
                .capacity(req.getCapacity())
                .bannerImageUrl(bannerUrl)
                .studyType(req.getStudyType())
                .contentMd(req.getDetail())
                .ageMin(ageMin)
                .ageMax(ageMax);

        if (req.getMode() == StudyMode.offline && req.getLocation() != null) {
            LocationDto loc = req.getLocation();
            b.region1depthName(loc.getRegion1depthName())
                    .region2depthName(loc.getRegion2depthName())
                    .region3depthName(loc.getRegion3depthName())
                    .roadName(loc.getRoadName())
                    .mainBuildingNo(loc.getMainBuildingNo())
                    .subBuildingNo(loc.getSubBuildingNo())
                    .zoneNo(loc.getZoneNo())
                    .latitude(loc.getLatitude())
                    .longitude(loc.getLongitude());
        } else {
            b.region1depthName(null).region2depthName(null).region3depthName(null)
                    .roadName(null).mainBuildingNo(null).subBuildingNo(null).zoneNo(null)
                    .latitude(null).longitude(null);
        }

        StudyRecruitment study = b.build();

        if (req.getTraits() != null) {
            req.getTraits().stream()
                    .filter(s -> s != null && !s.trim().isBlank())
                    .map(String::trim)
                    .distinct()
                    .forEach(tr -> study.getTraits().add(
                            StudyRecruitmentTrait.builder()
                                    .studyRecruitment(study)
                                    .traitName(tr)
                                    .build()
                    ));
        }

        try {
            studyRecruitmentRepository.save(study);
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.STUDY_SAVE_FAILED);
        }

        /**
         * 팀 자동 생성
         * - 모집글이 생성되면 동시에 팀 엔티티를 생성한다.
         * - type: STUDY
         * - title: 모집글 제목
         * - leader: 모집글 작성자
         * - 생성과 동시에 리더를 TeamMember(LEADER)로 자동 추가한다.
         */
        teamCommandService.createTeamForRecruitment(
                study.getId(),
                TeamType.STUDY,
                study.getTitle(),
                user
        );

        participantRepository.save(StudyParticipant.builder()
                .studyRecruitment(study)
                .user(user)
                .role(ParticipantRole.LEADER)
                .build());

        LocationDto location = buildLocationForRead(study);

        return StudyRecruitmentCreateResponse.builder()
                .studyId(study.getId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .teamStatus(study.getTeamStatus())
                .title(study.getTitle())
                .expectedStart(study.getStartDate())
                .expectedMonth(study.getExpectedMonths())
                .mode(study.getMode())
                .location(location)
                .preferredAges(PreferredAgesDto.of(study.getAgeMin(), study.getAgeMax()))
                .capacity(study.getCapacity())
                .bannerImageUrl(study.getBannerImageUrl())
                .traits(study.getTraits().stream().map(StudyRecruitmentTrait::getTraitName).toList())
                .studyType(study.getStudyType())
                .detail(study.getContentMd())
                .build();
    }

    private void validatePreferredAges(PreferredAgesDto preferredAges) {
        if (preferredAges == null) return; // 연령 무관

        Integer min = preferredAges.getAgeMin();
        Integer max = preferredAges.getAgeMax();

        // 연령 무관인데 Dto만 있고 값은 null인 경우도 방어
        if (min == null && max == null) return;

        if (min == null || max == null) {
            throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
        }
        if (min < 10 || max > 110) { // 100대까지
            throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
        }
        if (min >= max) {
            throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
        }
        if (min % 10 != 0 || max % 10 != 0) {
            throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
        }
    }

    private String uploadBannerImage(MultipartFile file) {
        try {
            return fileService.upload(file);
        } catch (IOException e) {
            throw new GlobalException(ErrorCode.BANNER_UPLOAD_FAILED);
        }
    }

    /** 신청 토글 */
    @Transactional
    public boolean toggleJoin(CustomUserDetails userDetails, Long studyId) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = userDetails.getUser().getId();
        String nickname = userDetails.getUser().getNickname();

        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));
        if (study.getDeletedAt() != null) throw new GlobalException(ErrorCode.STUDY_NOT_FOUND);
        if (study.getUser().getId().equals(userId)) throw new GlobalException(ErrorCode.FORBIDDEN_ACTION);

        Optional<StudyApplication> existingOpt =
                studyApplicationRepository.findByUser_IdAndStudyRecruitment_Id(userId, studyId);

        if (existingOpt.isPresent()) {
            StudyApplication existing = existingOpt.get();
            switch (existing.getApplicationStatus()) {
                case PENDING -> {
                    int deleted = studyApplicationRepository.deleteIfPending(existing.getId());
                    if (deleted == 0) throw new GlobalException(ErrorCode.APPLICATION_ALREADY_APPROVED);
                    return false;
                }
                case APPROVED -> throw new GlobalException(ErrorCode.APPLICATION_ALREADY_APPROVED);
                case REJECTED -> {
                    if (study.getTeamStatus() != TeamStatus.RECRUITING) {
                        throw new GlobalException(ErrorCode.RECRUITMENT_CLOSED);
                    }
                    int updated = studyApplicationRepository.reapplyIfRejected(existing.getId());
                    if (updated == 0) throw new GlobalException(ErrorCode.APPLICATION_ALREADY_APPROVED);

                    eventPublisher.publishEvent(
                            StudyJoinRequestedEvent.builder()
                                    .applicationId(existing.getId())
                                    .applicantUserId(userId)
                                    .applicantNickname(nickname)
                                    .studyId(study.getId())
                                    .studyTitle(study.getTitle())
                                    .ownerUserId(study.getUser().getId())
                                    .build()
                    );
                    return true;
                }
            }
            return false;
        }

        // 신규 신청
        if (study.getTeamStatus() != TeamStatus.RECRUITING) {
            throw new GlobalException(ErrorCode.RECRUITMENT_CLOSED);
        }

        StudyApplication newApp = StudyApplication.builder()
                .user(userDetails.getUser())
                .studyRecruitment(study)
                .applicationStatus(ApplicationStatus.PENDING)
                .build();

        studyApplicationRepository.save(newApp);

        eventPublisher.publishEvent(
                StudyJoinRequestedEvent.builder()
                        .applicationId(newApp.getId())
                        .applicantUserId(userId)
                        .applicantNickname(nickname)
                        .studyId(study.getId())
                        .studyTitle(study.getTitle())
                        .ownerUserId(study.getUser().getId())
                        .build()
        );

        return true; // 신청됨
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
}