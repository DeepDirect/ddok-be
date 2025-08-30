package goorm.ddok.project.dto.request;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.project.domain.ProjectMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(
        name = "ProjectCreateRequest",
        description = """
            프로젝트 모집글 생성 요청 DTO.
            - ONLINE 모드: location 생략 가능
            - OFFLINE 모드: location 필수 (카카오 road_address 매핑 필드 사용)
            - preferredAges는 null(무관) 가능
            """,
        requiredProperties = {
                "title", "expectedStart", "expectedMonth",
                "mode", "capacity", "positions", "leaderPosition", "detail"
        },
        example = """
        {
          "title": "구지라지",
          "expectedStart": "2025-09-16",
          "expectedMonth": 3,
          "mode": "OFFLINE",
          "location": {
            "address": "전북 익산시 부송동 망산길 11-17",
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
          "preferredAges": {
            "ageMin": 20,
            "ageMax": 30
          },
          "capacity": 6,
          "traits": ["정리의 신", "실행력 갓", "내향인"],
          "positions": ["백엔드", "프론트엔드", "디자이너"],
          "leaderPosition": "백엔드",
          "detail": "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?"
        }
        """
)
public class ProjectRecruitmentCreateRequest {

    @NotBlank(message = "제목은 필수 입력값입니다.")
    @Size(min = 2, max = 30, message = "제목은 2자 이상 30자 이하여야 합니다.")
    @Schema(description = "공고 제목", example = "구지라지", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "예상 시작일은 필수 입력값입니다.")
    @Schema(description = "프로젝트 시작 예정일(오늘 이후)", example = "2025-09-16", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate expectedStart;

    @NotNull(message = "예상 진행 개월 수는 필수 입력값입니다.")
    @Min(value = 1, message = "예상 진행 개월 수는 최소 1개월 이상이어야 합니다.")
    @Max(value = 64, message = "예상 진행 개월 수는 최대 64개월 이하이어야 합니다.")
    @Schema(description = "예상 진행 개월 수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedMonth;

    @NotNull(message = "프로젝트 진행 방식은 필수 입력값입니다.")
    @Schema(description = "진행 방식 (ONLINE / OFFLINE)", example = "OFFLINE", requiredMode = Schema.RequiredMode.REQUIRED)
    private ProjectMode mode;

    @Schema(description = """
            프로젝트 진행 장소.
            - OFFLINE일 때만 필요 (Kakao road_address 매핑)
            - ONLINE일 때는 null 또는 생략
            """,
            example = """
            {
              "address": "전북 익산시 부송동 망산길 11-17",
              "region1depthName": "전북",
              "region2depthName": "익산시",
              "region3depthName": "부송동",
              "roadName": "망산길",
              "mainBuildingNo": "11",
              "subBuildingNo": "17",
              "zoneNo": "54547",
              "latitude": 35.976749396987046,
              "longitude": 126.99599512792346
            }
            """)
    private LocationDto location;

    @Schema(description = "선호 연령대 (무관 시 null). 서비스에서 10단위(…0)만 허용.", example = "{\"ageMin\":20, \"ageMax\":30}")
    private PreferredAgesDto preferredAges;

    @NotNull(message = "모집 정원은 필수 입력값입니다.")
    @Min(value = 1, message = "모집 정원은 최소 1명 이상이어야 합니다.")
    @Max(value = 7, message = "모집 정원은 최대 7명 이하이어야 합니다.")
    @Schema(description = "모집 정원", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @Schema(description = "모집 성향 리스트", example = "[\"정리의 신\", \"실행력 갓\", \"내향인\"]")
    private List<String> traits;

    @NotEmpty(message = "모집 포지션은 최소 1개 이상이어야 합니다.")
    @Schema(description = "모집 포지션 리스트(중복/공백은 서비스에서 정규화)", example = "[\"백엔드\", \"프론트엔드\", \"디자이너\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> positions;

    @NotBlank(message = "리더 포지션은 필수 입력값입니다.")
    @Schema(description = "리더 포지션(positions에 포함되어야 함)", example = "백엔드", requiredMode = Schema.RequiredMode.REQUIRED)
    private String leaderPosition;

    @NotBlank(message = "상세 설명은 필수 입력값입니다.")
    @Size(min = 10, max = 2000)
    @Schema(description = "상세 설명 (Markdown)", example = "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String detail;
}