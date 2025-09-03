package goorm.ddok.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "LocationRequest",
        description = "사용자 위치 정보 요청 DTO (Kakao road_address 매핑)",
        requiredProperties = {"latitude", "longitude"},
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
          "latitude": 35.976749,
          "longitude": 126.995995
        }
        """
)
public class LocationRequest {

    /* ---- 주소 컴포넌트 (nullable 허용) ---- */
    @Schema(description = "전체 주소(서버에서 조립해서 응답할 수 있음, 요청 시 null 허용)", example = "전북 익산시 부송동 망산길 11-17")
    private String address; // CHANGED: nullable 허용, 서버는 컴포넌트 기반 저장

    @Schema(description = "시/도(1뎁스)", example = "전북")
    private String region1depthName; // CHANGED

    @Schema(description = "시/군/구(2뎁스)", example = "익산시")
    private String region2depthName; // CHANGED

    @Schema(description = "읍/면/동(3뎁스)", example = "부송동")
    private String region3depthName; // CHANGED

    @Schema(description = "도로명", example = "망산길")
    private String roadName; // CHANGED

    @Schema(description = "건물 본번", example = "11")
    private String mainBuildingNo; // CHANGED

    @Schema(description = "건물 부번", example = "17")
    private String subBuildingNo; // CHANGED

    @Schema(description = "우편번호(도로명주소 zoneNo)", example = "54547")
    private String zoneNo; // CHANGED

    /* ---- 위/경도(필수) ---- */
    @Schema(description = "위도", example = "37.5665", minimum = "-90.0", maximum = "90.0",
            type = "number", format = "double", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "위도는 필수 입력 값입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780", minimum = "-180.0", maximum = "180.0",
            type = "number", format = "double", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "경도는 필수 입력 값입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
    private Double longitude;
}