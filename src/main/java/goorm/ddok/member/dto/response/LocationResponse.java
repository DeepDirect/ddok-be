package goorm.ddok.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        name = "LocationResponse",
        description = "사용자 위치 정보 응답 DTO",
        example = """
        {
          "address": null,
          "region1depthName": "전북",
          "region2depthName": "익산시",
          "region3depthName": "부송동",
          "roadName": "망산길",
          "mainBuildingNo": "11",
          "subBuildingNo": "17",
          "zoneNo": "54547",
          "latitude": 37.566500,
          "longitude": 126.978000
        }
        """
)
public record LocationResponse(

        // CHANGED: 주소 전체(조립값; 없으면 null)
        @Schema(description = "전체 주소(서버에서 조립)", example = "전북 익산시 부송동 망산길 11-17",
                accessMode = Schema.AccessMode.READ_ONLY)
        String address,

        // CHANGED: 컴포넌트들
        @Schema(description = "시/도(1뎁스)", example = "전북", accessMode = Schema.AccessMode.READ_ONLY)
        String region1depthName,

        @Schema(description = "시/군/구(2뎁스)", example = "익산시", accessMode = Schema.AccessMode.READ_ONLY)
        String region2depthName,

        @Schema(description = "읍/면/동(3뎁스)", example = "부송동", accessMode = Schema.AccessMode.READ_ONLY)
        String region3depthName,

        @Schema(description = "도로명", example = "망산길", accessMode = Schema.AccessMode.READ_ONLY)
        String roadName,

        @Schema(description = "건물 본번", example = "11", accessMode = Schema.AccessMode.READ_ONLY)
        String mainBuildingNo,

        @Schema(description = "건물 부번", example = "17", accessMode = Schema.AccessMode.READ_ONLY)
        String subBuildingNo,

        @Schema(description = "우편번호(도로명주소 zoneNo)", example = "54547", accessMode = Schema.AccessMode.READ_ONLY)
        String zoneNo,

        // CHANGED: 위/경도는 BigDecimal 유지
        @Schema(description = "위도", example = "37.566500", minimum = "-90.0", maximum = "90.0",
                type = "number", format = "double", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.978000", minimum = "-180.0", maximum = "180.0",
                type = "number", format = "double", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal longitude
) {}