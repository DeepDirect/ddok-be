package goorm.ddok.team.dto.response;

import goorm.ddok.chat.dto.response.PaginationResponse;
import goorm.ddok.team.domain.TeamType;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamApplicantsResponse {

    private PaginationResponse pagination;
    private Long teamId;
    private Long recruitmentId;
    private boolean IsLeader;
    private TeamType teamType; // PROJECT or STUDY
    private List<TeamApplicantResponse> items;
}

