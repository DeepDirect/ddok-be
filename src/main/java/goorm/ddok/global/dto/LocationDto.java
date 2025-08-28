package goorm.ddok.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(name = "LocationDto", description = "카카오 road_address 매핑 DTO")
public class LocationDto {

    @Schema(description = "전체 도로명 주소 (address_name)",
            example = "전북 익산시 망산길 11-17")
    private String address;

    @Schema(description = "광역시/도 (region_1depth_name)",
            example = "전북")
    private String region1depthName;

    @Schema(description = "시/군/구 (region_2depth_name)",
            example = "익산시")
    private String region2depthName;

    @Schema(description = "동/읍/면 (region_3depth_name)",
            example = "부송동")
    private String region3depthName;

    @Schema(description = "도로명 (road_name)",
            example = "망산길")
    private String roadName;

    @Schema(description = "우편번호(필요 시) (zone_no)",
            example = "54547")
    private String zoneNo;

    @Schema(description = "위도(y)",
            example = "35.976749396987046")
    private BigDecimal latitude;   // kakao y

    @Schema(description = "경도(x)",
            example = "126.99599512792346")
    private BigDecimal longitude;  // kakao x

    @Schema(description = "건물 본번 (main_building_no)",
            example = "11")
    private String mainBuildingNo;

    @Schema(description = "건물 부번 (sub_building_no)",
            example = "17")
    private String subBuildingNo;
}