package goorm.ddok.study.dto.request;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.TeamStatus;
import goorm.ddok.study.domain.StudyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(name = "StudyRecruitmentUpdateRequest", description = "스터디 수정 요청 DTO")
public class StudyRecruitmentUpdateRequest {

    @NotBlank @Size(min = 2, max = 30)
    @Schema(example = "구지라지") private String title;

    @NotNull
    @Schema(example = "RECRUITING") private TeamStatus teamStatus;

    @NotNull
    @Schema(example = "2025-08-16") private LocalDate expectedStart;

    @NotNull @Min(1) @Max(64)
    @Schema(example = "3") private Integer expectedMonth;

    @NotNull
    @Schema(example = "offline") private StudyMode mode;

    @Schema(description = "OFFLINE 필수") private LocationDto location;

    @Schema(description = "없으면 null") private PreferredAgesDto preferredAges;

    @NotNull @Min(1) @Max(100)
    @Schema(example = "6") private Integer capacity;

    @Schema(example = "[\"정리의 신\",\"실행력 갓\",\"내향인\"]")
    private List<String> traits;

    @NotNull
    @Schema(example = "SELF_DEV")
    private StudyType studyType;

    @NotBlank @Size(min = 10, max = 2000)
    @Schema(example = "저희 정말 멋진 영어공부를 할거예요~ 하고 싶죠?")
    private String detail;

    @Schema(description = "배너 이미지 URL (파일 미첨부 시 유지/변경 위해 전송 가능)", example = "https://cdn.example.com/images/default.png")
    private String bannerImageUrl;
}