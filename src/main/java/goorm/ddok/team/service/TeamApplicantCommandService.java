package goorm.ddok.team.service;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.project.domain.ProjectApplication;
import goorm.ddok.project.domain.ProjectParticipant;
import goorm.ddok.project.domain.ProjectRecruitment;
import goorm.ddok.project.repository.ProjectApplicationRepository;
import goorm.ddok.project.repository.ProjectParticipantRepository;
import goorm.ddok.study.domain.ParticipantRole;
import goorm.ddok.study.domain.StudyApplication;
import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.team.domain.Team;
import goorm.ddok.team.domain.TeamMemberRole;
import goorm.ddok.team.domain.TeamType;
import goorm.ddok.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamApplicantCommandService {

    private final StudyApplicationRepository studyApplicationRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final TeamRepository teamRepository;

    /**
     * 신청 승인 처리
     *
     * @param teamId        승인할 팀 ID
     * @param applicationId 신청 ID
     * @param user          현재 로그인한 사용자 (리더만 가능)
     */
    public void approve(Long teamId, Long applicationId, CustomUserDetails user) {
        Team team = validateLeader(teamId, user);

        if (team.getType() == TeamType.STUDY) {
            approveStudyApplication(team, applicationId);
        } else if (team.getType() == TeamType.PROJECT) {
            approveProjectApplication(team, applicationId);
        } else {
            throw new GlobalException(ErrorCode.INVALID_TEAM_TYPE);
        }
    }

    /**
     * 신청 거절 처리
     *
     * @param teamId        거절할 팀 ID
     * @param applicationId 신청 ID
     * @param user          현재 로그인한 사용자 (리더만 가능)
     */
    public void reject(Long teamId, Long applicationId, CustomUserDetails user) {
        Team team = validateLeader(teamId, user);

        if (team.getType() == TeamType.STUDY) {
            rejectStudyApplication(applicationId);
        } else if (team.getType() == TeamType.PROJECT) {
            rejectProjectApplication(applicationId);
        } else {
            throw new GlobalException(ErrorCode.INVALID_TEAM_TYPE);
        }
    }

    /**
     * 리더 권한 검증
     *
     * @param teamId 검증할 팀 ID
     * @param user   현재 로그인한 사용자
     * @return 리더 권한이 확인된 Team 엔티티
     */
    private Team validateLeader(Long teamId, CustomUserDetails user) {
        if (user == null || user.getUser() == null) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new GlobalException(ErrorCode.TEAM_NOT_FOUND));

        if (!team.getUser().getId().equals(user.getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN_LEADER_ONLY);
        }
        return team;
    }

    /**
     * 스터디 신청 승인 처리
     *
     * @param team          승인 대상 팀
     * @param applicationId 신청 ID
     */
    private void approveStudyApplication(Team team, Long applicationId) {
        StudyApplication application = studyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.isPending()) {
            throw new GlobalException(ErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        StudyRecruitment recruitment = application.getStudyRecruitment();

        long participantCount =
                studyParticipantRepository.countByStudyRecruitment_IdAndDeletedAtIsNull(recruitment.getId());
        if (participantCount >= recruitment.getCapacity()) {
            throw new GlobalException(ErrorCode.CAPACITY_EXCEEDED);
        }

        application.approve();

        StudyParticipant participant = StudyParticipant.builder()
                .studyRecruitment(recruitment)
                .user(application.getUser())
                .role(ParticipantRole.MEMBER)
                .build();
        studyParticipantRepository.save(participant);

        team.addMember(application.getUser(), TeamMemberRole.MEMBER);
    }

    /**
     * 스터디 신청 거절 처리
     *
     * @param applicationId 신청 ID
     */
    private void rejectStudyApplication(Long applicationId) {
        StudyApplication application = studyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.isPending()) {
            throw new GlobalException(ErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        application.reject();
    }

    /**
     * 프로젝트 신청 승인 처리
     *
     * @param team          승인 대상 팀
     * @param applicationId 신청 ID
     */
    private void approveProjectApplication(Team team, Long applicationId) {
        ProjectApplication application = projectApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.isPending()) {
            throw new GlobalException(ErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        ProjectRecruitment recruitment = application.getPosition().getProjectRecruitment();
        long participantCount =
                projectParticipantRepository.countByPosition_ProjectRecruitment_IdAndDeletedAtIsNull(recruitment.getId());

        if (participantCount >= recruitment.getCapacity()) {
            throw new GlobalException(ErrorCode.CAPACITY_EXCEEDED);
        }

        application.approve();

        ProjectParticipant participant = ProjectParticipant.builder()
                .user(application.getUser())
                .position(application.getPosition())
                .role(goorm.ddok.project.domain.ParticipantRole.MEMBER)
                .build();
        projectParticipantRepository.save(participant);

        team.addMember(application.getUser(), TeamMemberRole.MEMBER);
    }

    /**
     * 프로젝트 신청 거절 처리
     *
     * @param applicationId 신청 ID
     */
    private void rejectProjectApplication(Long applicationId) {
        ProjectApplication application = projectApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.isPending()) {
            throw new GlobalException(ErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        application.reject();
    }
}