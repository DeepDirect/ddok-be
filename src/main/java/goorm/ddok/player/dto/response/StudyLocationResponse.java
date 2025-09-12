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
        description = "스터디 위치 DTO (LocationDto 통일)",
        example = """
        {
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
        }
        """
)
public class StudyLocationResponse {

    private String address;
    private String region1depthName;
    private String region2depthName;
    private String region3depthName;
    private String roadName;
    private String mainBuildingNo;
    private String subBuildingNo;
    private String zoneNo;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static StudyLocationResponse from(StudyRecruitment study) {
        String region1 = study.getRegion1depthName() != null ? study.getRegion1depthName() : "";
        String region2 = study.getRegion2depthName() != null ? study.getRegion2depthName() : "";
        String address = normalizeRegion1(region1) + " " + region2;

        return StudyLocationResponse.builder()
                .address(address)
                .region1depthName(study.getRegion1depthName())
                .region2depthName(study.getRegion2depthName())
                .region3depthName(study.getRegion3depthName())
                .roadName(study.getRoadName())
                .mainBuildingNo(study.getMainBuildingNo())
                .subBuildingNo(study.getSubBuildingNo())
                .zoneNo(study.getZoneNo())
                .latitude(study.getLatitude())
                .longitude(study.getLongitude())
                .build();
    }

    private static String normalizeRegion1(String region1) {
        return region1.replace("특별시", "")
                .replace("광역시", "")
                .replace("자치시", "")
                .replace("도", "")
                .trim();
    }
}