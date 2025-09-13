package goorm.ddok.player.dto.response;

import goorm.ddok.project.domain.ProjectParticipant;
import goorm.ddok.project.domain.ProjectRecruitment;
import goorm.ddok.project.domain.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "ProjectParticipationResponse",
        description = "참여 프로젝트 조회 응답 DTO",
        example = """
        {
          "projectId": 1,
          "teamId": 2,
          "title": "구지라지 프로젝트",
          "teamStatus": "CLOSED",
          "location": {
          "address": "전북 익산시",
            "region1depthName": "전북",
            "region2depthName": "익산시",
            "region3depthName": "부송동",
            "roadName": "망산길",
            "mainBuildingNo": "11",
            "subBuildingNo": "17",
            "zoneNo": "54547",
            "latitude": 35.976749396987046,
            "longitude": 126.99599512792346
          },
          "period": {
            "start": "2025-08-01",
            "end": "2025-09-15"
          }
        }
        """
)
public class ProjectParticipationResponse {

    @Schema(description = "프로젝트 ID", example = "1")
    private Long projectId;

    @Schema(description = "팀 ID", example = "2")
    private Long teamId;

    @Schema(description = "프로젝트 제목", example = "구지라지 프로젝트")
    private String title;

    @Schema(description = "팀 상태 (ONGOING / CLOSED)", example = "CLOSED")
    private String teamStatus;

    @Schema(description = "프로젝트 위치 정보")
    private ProjectLocationResponse location;

    @Schema(description = "진행 기간 (시작일 ~ 종료일)")
    private ProjectPeriodResponse period;

    public static ProjectParticipationResponse from(ProjectParticipant participant, Long teamId, String statusGroup) {
        ProjectRecruitment project = participant.getPosition().getProjectRecruitment();

        return ProjectParticipationResponse.builder()
                .projectId(project.getId())
                .teamId(teamId)
                .title(project.getTitle())
                .teamStatus(statusGroup)
                .location(ProjectLocationResponse.from(project))
                .period(ProjectPeriodResponse.from(
                        project.getStartDate(),
                        project.getExpectedMonths(),
                        project.getTeamStatus()
                ))
                .build();
    }
}
