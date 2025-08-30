package goorm.ddok.study.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_recruitment")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class StudyRecruitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 리더 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User user;

    /** 제목 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "team_status", nullable = false, length = 20)
    private TeamStatus teamStatus;

    /** 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 예상 기간(개월 단위) */
    @Column(name = "expected_months", nullable = false)
    private Integer expectedMonths;

    /** 모드 (온라인/오프라인) */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private StudyMode mode;

    /** 카카오 road_address/region 필드 */
    @Column(name = "region1_depth_name", length = 50)
    private String region1DepthName;

    @Column(name = "region2_depth_name", length = 50)
    private String region2DepthName;

    @Column(name = "region3_depth_name", length = 50)
    private String region3DepthName;

    @Column(name = "road_name", length = 100)
    private String roadName;

    @Column(name = "main_building_no", length = 20)
    private String mainBuildingNo;

    @Column(name = "sub_building_no", length = 20)
    private String subBuildingNo;

    @Column(name = "zone_no", length = 20)
    private String zoneNo;

    @Column(name = "latitude", precision = 14, scale = 10)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 14, scale = 10)
    private BigDecimal longitude;

    /** 나이 제한 (무관: 0/0) */
    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    /** 정원 */
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /** 배너 이미지 */
    @Column(name = "banner_image_url", nullable = false, length = 1024)
    private String bannerImageUrl;

    /** 스터디 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "study_type", nullable = false, length = 30)
    private StudyType studyType;

    /** 본문 */
    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    /** 시간 정보 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** 연관관계 */
    @OneToMany(mappedBy = "studyRecruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyRecruitmentTrait> traits = new ArrayList<>();

    @OneToMany(mappedBy = "studyRecruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "studyRecruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudyParticipant> participants = new ArrayList<>();
}