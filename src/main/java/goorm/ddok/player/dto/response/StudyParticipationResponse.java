package goorm.ddok.player.dto.response;

import goorm.ddok.study.domain.ParticipantRole;
import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.domain.TeamStatus;
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
        name = "StudyParticipationResponse",
        description = "참여 스터디 조회 응답 DTO",
        example = """
        {
          "studyId": 1,
          "title": "면접 스터디",
          "teamStatus": "CLOSED",
          "role": "MEMBER",
          "location": {
            "latitude": 37.5665,
            "longitude": 126.9780,
            "address": "서울특별시 강남구 테헤란로…"
          },
          "recruitmentPeriod": {
            "start": "2025-08-08",
            "end": "2025-09-09"
          }
        }
        """
)
public class StudyParticipationResponse {

    @Schema(description = "스터디 ID", example = "1")
    private Long studyId;

    @Schema(description = "스터디 제목", example = "면접 스터디")
    private String title;

    @Schema(description = "팀 상태 (RECRUITING / ONGOING / CLOSED)", example = "CLOSED")
    private TeamStatus teamStatus;


    @Schema(description = "스터디 장소 정보 (OFFLINE일 경우만 존재)")
    private StudyLocationResponse location;

    @Schema(description = "모집 기간 (시작일 ~ 종료일)")
    private RecruitmentPeriodResponse recruitmentPeriod;

    public static StudyParticipationResponse from(StudyParticipant participant) {
        System.out.println("DEBUG role = " + participant.getRole());
        StudyRecruitment study = participant.getStudyRecruitment();

        return StudyParticipationResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .teamStatus(study.getTeamStatus())
                .location(StudyLocationResponse.from(study))
                .recruitmentPeriod(RecruitmentPeriodResponse.from(
                        study.getStartDate(),
                        study.getExpectedMonths(),
                        study.getTeamStatus()
                ))
                .build();
    }
}