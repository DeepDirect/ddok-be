package goorm.ddok.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
        name = "LocationResponse",
        description = "사용자 위치 정보 응답 DTO",
        example = """
    {
      "latitude": 37.5665,
      "longitude": 126.9780,
      "address": "서울특별시 강남구 테헤란로 123"
    }
    """
)
public record LocationResponse(
        @Schema(
                description = "위도",
                example = "37.5665",
                minimum = "-90.0",
                maximum = "90.0",
                type = "number",
                format = "double",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        BigDecimal latitude,

        @Schema(
                description = "경도",
                example = "126.9780",
                minimum = "-180.0",
                maximum = "180.0",
                type = "number",
                format = "double",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        BigDecimal longitude,

        @Schema(
                description = "주소",
                example = "서울특별시 강남구 테헤란로 123",
                type = "string",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String address
) {}
