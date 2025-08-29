package goorm.ddok.player.dto.response;

import goorm.ddok.study.domain.StudyRecruitment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "StudyLocationResponse",
        description = "스터디 위치 DTO",
        example = """
        {
          "latitude": 37.5665,
          "longitude": 126.9780,
          "address": "서울특별시 강남구 테헤란로…"
        }
        """
)
public class StudyLocationResponse {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;

    public static StudyLocationResponse from(StudyRecruitment study) {
        String address = String.join(" ",
                study.getRegion1DepthName() != null ? study.getRegion1DepthName() : "",
                study.getRegion2DepthName() != null ? study.getRegion2DepthName() : "",
                study.getRegion3DepthName() != null ? study.getRegion3DepthName() : "",
                study.getRoadName() != null ? study.getRoadName() : ""
        ).trim();

        return StudyLocationResponse.builder()
                .latitude(study.getLatitude())
                .longitude(study.getLongitude())
                .address(address.isEmpty() ? null : address)
                .build();
    }
}