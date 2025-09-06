package goorm.ddok.team.dto.response;

import goorm.ddok.project.domain.ProjectApplication;
import goorm.ddok.study.domain.StudyApplication;
import goorm.ddok.team.domain.ApplicantStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamApplicantResponse {

    private Long applicantId;
    /* Study일 경우 null */
    private String appliedPosition;

    @Enumerated(EnumType.STRING)
    private ApplicantStatus status;

    private Instant appliedAt;
    private boolean IsMine;
    private TeamApplicantUserResponse user;

}
