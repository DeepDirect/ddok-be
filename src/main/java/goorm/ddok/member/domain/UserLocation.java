package goorm.ddok.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "region_1depth_name", length = 50)
    private String region1DepthName;

    @Column(name = "region_2depth_name", length = 50)
    private String region2DepthName;

    @Column(name = "region_3depth_name", length = 50)
    private String region3DepthName;

    @Column(name = "road_name", length = 50)
    private String roadName;

    @Schema(description = "건물 본번 (main_building_no)",
            example = "11")
    private String mainBuildingNo;

    @Schema(description = "건물 부번 (sub_building_no)",
            example = "17")
    private String subBuildingNo;

    @Column(name = "zone_no", length = 50)
    private String zoneNo;

    @Column(name = "kakao_place_id", length = 64)
    private String kakaoPlaceId;

    /** 위도/경도: numeric(9,6) */
    @Column(name = "activity_latitude", precision = 9, scale = 6)
    private BigDecimal activityLatitude;

    @Column(name = "activity_longitude", precision = 9, scale = 6)
    private BigDecimal activityLongitude;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
