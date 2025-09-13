package goorm.ddok.player.dto.response;

import goorm.ddok.project.domain.ProjectRecruitment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "ProjectLocationResponse",
        description = "프로젝트 위치 DTO",
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
public class ProjectLocationResponse {
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

    public static ProjectLocationResponse from(ProjectRecruitment project) {
        if (project == null) return null;

        String r1 = Optional.ofNullable(project.getRegion1depthName()).orElse("").trim();
        String r2 = Optional.ofNullable(project.getRegion2depthName()).orElse("").trim();

        StringBuilder sb = new StringBuilder();
        if (!r1.isEmpty()) sb.append(r1).append(" ");
        if (!r2.isEmpty()) sb.append(r2);

        String address = sb.toString().trim().replaceAll("\\s+", " ");

        return ProjectLocationResponse.builder()
                .address(address.isBlank() ? null : address)
                .region1depthName(project.getRegion1depthName())
                .region2depthName(project.getRegion2depthName())
                .region3depthName(project.getRegion3depthName())
                .roadName(project.getRoadName())
                .mainBuildingNo(project.getMainBuildingNo())
                .subBuildingNo(project.getSubBuildingNo())
                .zoneNo(project.getZoneNo())
                .latitude(project.getLatitude())
                .longitude(project.getLongitude())
                .build();
    }

}
