package goorm.ddok.player.dto.response;


import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.domain.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "RecruitmentPeriodResponse",
        description = "스터디 기간 정보",
        example = """
        {
          "start": "2025-06-04",
          "end": "2025-08-12"
        }
        """
)
public class RecruitmentPeriodResponse {

    @Schema(description = "스터디 시작일", example = "2025-06-04")
    private LocalDate start;

    @Schema(description = "스터디 종료일 (진행 중이면 null)", example = "2025-08-12")
    private LocalDate end;


    public static RecruitmentPeriodResponse from(LocalDate startDate, Integer expectedMonths, TeamStatus status) {
        LocalDate end = null;

        // TODO: team 도메인 추가되면 실제 종료일 컬럼 사용하도록 수정
        if (status == TeamStatus.CLOSED && expectedMonths != null) {
            end = startDate.plusMonths(expectedMonths);
        }

        return RecruitmentPeriodResponse.builder()
                .start(startDate)
                .end(end)
                .build();
    }
}