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
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.domain.*;
import goorm.ddok.study.dto.UserSummaryDto;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import goorm.ddok.team.domain.Team;
import goorm.ddok.team.domain.TeamType;
import goorm.ddok.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyRecruitmentQueryService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final StudyApplicationRepository studyApplicationRepository;
    private final UserReputationRepository userReputationRepository;
    private final TeamRepository teamRepository;
    private final BadgeService badgeService;
    private final ChatRoomService chatRoomService;
    private final DmRequestCommandService dmRequestService;


    /** 스터디 상세 조회 (수정페이지와 동일 스키마) */
    public StudyRecruitmentDetailResponse getStudyDetail(Long studyId, CustomUserDetails userDetails) {
        // 1) 소프트삭제 확인
        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));
        if (study.getDeletedAt() != null) throw new GlobalException(ErrorCode.STUDY_NOT_FOUND);

        User me = (userDetails != null) ? userDetails.getUser() : null;

        // 2) 참가자(리더+멤버)
        List<StudyParticipant> all = studyParticipantRepository.findByStudyRecruitment_IdAndDeletedAtIsNull(study.getId());

        // 3) 리더 필수
        StudyParticipant leader = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.LEADER_NOT_FOUND));

        // 4) 멤버/지원자 수
        List<StudyParticipant> members = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .toList();
        int participantsCount = members.size();
        int applicantCount = (int) studyApplicationRepository
                .countByStudyRecruitmentAndApplicationStatusNot(study, ApplicationStatus.REJECTED);

        // 5) 리더/멤버 요약 (온도/뱃지 없으면 null)
        UserSummaryDto leaderDto = toUserSummaryDto(leader, me);
        List<UserSummaryDto> participantDtos = members.stream()
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getUser().getNickname()).orElse("")))
                .map(p -> toUserSummaryDto(p, me))
                .toList();

        // 6) 주소(online → null), 선호연령(0/0 → null)
        LocationDto location = buildLocationForRead(study);
        PreferredAgesDto ages = PreferredAgesDto.of(
                study.getAgeMin(),
                study.getAgeMax()
        );


        boolean isApplied = false;
        boolean isApproved = false;

        if (me != null) {
            Long meId = me.getId();

            boolean isParticipant = all.stream()
                    .anyMatch(p -> Objects.equals(p.getUser().getId(), meId) && p.getDeletedAt() == null);

            boolean hasApprovedApplication =
                    studyApplicationRepository.existsByUser_IdAndStudyRecruitment_IdAndApplicationStatus(
                            meId, studyId, ApplicationStatus.APPROVED
                    );

            isApproved = isParticipant || hasApprovedApplication;

            if (!isApproved) {
                isApplied = studyApplicationRepository.existsByUser_IdAndStudyRecruitment_IdAndApplicationStatus(
                        meId, studyId, ApplicationStatus.PENDING
                );
            }
        }

        Optional<Team> team = teamRepository.findByRecruitmentIdAndType(study.getId(), TeamType.STUDY);

        if (team.isEmpty()) {
            throw new GlobalException(ErrorCode.TEAM_NOT_FOUND);
        }

        // 7) 응답
        return StudyRecruitmentDetailResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .teamId(team.get().getId())
                .IsTeamMember(me != null && team.get().getMembers().stream()
                        .anyMatch(p -> p.getUser().getId().equals(me.getId()) && p.getDeletedAt() == null))
                .studyType(study.getStudyType())
                .IsMine(me != null && Objects.equals(study.getUser().getId(), me.getId()))
                .teamStatus(study.getTeamStatus())
                .bannerImageUrl(study.getBannerImageUrl())
                .traits(study.getTraits().stream().map(t -> t.getTraitName()).toList())
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
                .IsApplied(isApplied)
                .IsApproved(isApproved)
                .build();
    }

    /* ===== helpers ===== */

    private boolean isZero(Integer v) { return v == null || v == 0; }


    /** 요약 변환: 온도/뱃지 “없으면 null” */
    private UserSummaryDto toUserSummaryDto(StudyParticipant participant, User me) {
        User u = participant.getUser();

        BadgeDto mainBadge = badgeService.getRepresentativeGoodBadge(u);
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(u);

        String mainPosition = u.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        // 온도: 없으면 null
        BigDecimal temperature = userReputationRepository.findByUserId(u.getId())
                .map(UserReputation::getTemperature)
                .orElse(null);

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
                .mainBadge(mainBadge)
                .abandonBadge(abandonBadge)
                .temperature(temperature)      // null 허용
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
}