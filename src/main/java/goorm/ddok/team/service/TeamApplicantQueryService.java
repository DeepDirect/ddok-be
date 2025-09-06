package goorm.ddok.team.service;

import goorm.ddok.badge.service.BadgeService;
import goorm.ddok.chat.dto.response.PaginationResponse;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.project.domain.ProjectApplication;
import goorm.ddok.project.repository.ProjectApplicationRepository;
import goorm.ddok.study.domain.StudyApplication;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.team.domain.Team;
import goorm.ddok.team.domain.TeamType;
import goorm.ddok.team.dto.response.TeamApplicantResponse;
import goorm.ddok.team.dto.response.TeamApplicantUserResponse;
import goorm.ddok.team.dto.response.TeamApplicantsResponse;
import goorm.ddok.team.repository.TeamMemberRepository;
import goorm.ddok.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamApplicantQueryService {

    private final StudyApplicationRepository studyApplicationRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final BadgeService badgeService;

    public TeamApplicantsResponse getApplicants(
            Long teamId,
            CustomUserDetails user,
            int page, int size) {

        // 로그인 체크
        if (user == null || user.getUser() == null) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }

        Long currentUserId = user.getId();

        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new GlobalException(ErrorCode.TEAM_NOT_FOUND));

        // 팀 멤버 여부 체크 (승인된 멤버만)
        boolean isTeamMember = teamMemberRepository
                .existsByTeam_IdAndUser_IdAndDeletedAtIsNull(teamId, currentUserId);

        if (!isTeamMember) {
            throw new GlobalException(ErrorCode.FORBIDDEN_TEAM_ACCESS);
        }

        TeamType teamType = team.getType();
        boolean IsLeader = team.getUser().getId().equals(currentUserId);
        Long recruitmentId = team.getRecruitmentId();

        // STUDY
        if (teamType == TeamType.STUDY) {
            Page<StudyApplication> apps =
                    studyApplicationRepository.findByStudyRecruitment_IdAndApplicationStatus(
                            recruitmentId,
                            goorm.ddok.study.domain.ApplicationStatus.PENDING,
                            PageRequest.of(page, size)
                    );

            List<TeamApplicantResponse> items = apps.stream()
                    .map(app -> TeamApplicantResponse.builder()
                            .applicantId(app.getId())
                            .appliedPosition(null) // 스터디는 포지션 없음
                            .status(goorm.ddok.team.domain.ApplicantStatus.from(app.getApplicationStatus()))
                            .appliedAt(app.getCreatedAt())
                            .IsMine(app.getUser().getId().equals(currentUserId))
                            .user(toUserResponse(app.getUser()))
                            .build()
                    ).toList();

            return TeamApplicantsResponse.builder()
                    .pagination(PaginationResponse.of(apps))
                    .teamId(team.getId())
                    .recruitmentId(recruitmentId)
                    .IsLeader(IsLeader)
                    .teamType(teamType)
                    .items(items)
                    .build();
        }

        if (teamType == TeamType.PROJECT) {
            Page<ProjectApplication> apps =
                    projectApplicationRepository.findByPosition_ProjectRecruitment_IdAndStatus(
                            recruitmentId,
                            goorm.ddok.project.domain.ApplicationStatus.PENDING,
                            PageRequest.of(page, size)
                    );

            List<TeamApplicantResponse> items = apps.stream()
                    .map(app -> TeamApplicantResponse.builder()
                            .applicantId(app.getId())
                            .appliedPosition(app.getPosition().getPositionName())
                            .status(goorm.ddok.team.domain.ApplicantStatus.from(app.getStatus()))
                            .appliedAt(app.getCreatedAt())
                            .IsMine(app.getUser().getId().equals(currentUserId))
                            .user(toUserResponse(app.getUser()))
                            .build()
                    ).toList();

            return TeamApplicantsResponse.builder()
                    .pagination(PaginationResponse.of(apps))
                    .teamId(team.getId())
                    .recruitmentId(recruitmentId)
                    .IsLeader(IsLeader)
                    .teamType(teamType)
                    .items(items)
                    .build();
        }

        throw new GlobalException(ErrorCode.INVALID_TEAM_TYPE);
    }

    /**
     * User -> TeamApplicantUserResponse 변환
     */
    private TeamApplicantUserResponse toUserResponse(goorm.ddok.member.domain.User user) {
        // 대표 배지: 착한 배지들 중 tier 가장 높은 것
        BadgeDto mainBadge = badgeService.getGoodBadges(user).stream()
                .max(Comparator.comparingInt(b -> b.getTier().ordinal()))
                .orElse(null);

        // 탈주 배지
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(user);

        return TeamApplicantUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .temperature(user.getReputation() != null ? user.getReputation().getTemperature() : BigDecimal.valueOf(36.5))
                .mainPosition(resolvePrimaryPosition(user))
                .chatRoomId(null)
                .dmRequestPending(false)
                .mainBadge(mainBadge)
                .abandonBadge(abandonBadge)
                .build();
    }

    private static String resolvePrimaryPosition(goorm.ddok.member.domain.User user) {
        return user.getPositions().stream()
                .filter(pos -> pos.getType() == goorm.ddok.member.domain.UserPositionType.PRIMARY)
                .map(goorm.ddok.member.domain.UserPosition::getPositionName)
                .findFirst()
                .orElse(null);
    }
}