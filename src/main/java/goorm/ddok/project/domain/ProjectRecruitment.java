package goorm.ddok.project.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "project_recruitment")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProjectRecruitment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 리더 사용자 (FK: user.id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User user;

    /** 공고 제목 (2~30자, 필수) */
    @Column(nullable = false, length = 100)
    private String title;

    /** 팀 상태 : RECRUITING / ONGOING / CLOSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus teamStatus;

    /** 프로젝트 시작 예정일 */
    @Column(nullable = false)
    private LocalDate startDate;

    /** 예상 진행 개월 수 (최소 1개월, 최대 64개월) */
    @Column(nullable = false)
    private int expectedMonths;

    /** 프로젝트 진행 방식 : ONLINE / OFFLINE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMode projectMode;

    /** 프로젝트 진행 장소 (광역시/도) */
    @Column(length = 50)
    private String region1depthName;

    /** 프로젝트 진행 장소 (시/군/구) */
    @Column(length = 50)
    private String region2depthName;

    /** 프로젝트 진행 장소 (동/읍/면) */
    @Column(length = 50)
    private String region3depthName;

    /** 프로젝트 진행 장소 (도로명 주소) */
    @Column(length = 50)
    private String roadName;

    /** 프로젝트 진행 장소 (건물 번호) */
    @Column(length = 16)
    private String mainBuildingNo;

    /** 프로젝트 진행 장소 (건물 부번호) */
    @Column(length = 16)
    private String subBuildingNo;

    /** 프로젝트 진행 장소 (우편번호) */
    @Column(length = 16)
    private String zoneNo;

    /** 위도 (소수점 6자리) */
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    /** 경도 (소수점 6자리) */
    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    /** 최소 연령 (무관이면 0) */
    @Column(nullable = false)
    private int ageMin;
    /** 최대 연령 (무관이면 0) */
    @Column(nullable = false)
    private int ageMax;

    /** 모집 정원 (1-7명) */
    @Column(nullable = false)
    private int capacity;

    /** 배너 이미지 URL (필수)*/
    @Column(nullable = false, length = 1024)
    private String bannerImageUrl;

    /** 공고 상세 설명 (10-2000 자) */
    @Column(nullable = false, columnDefinition = "TEXT", length = 2000)
    private String contentMd;

    /** 생성 시각 (자동 입력) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 최종 수정 시각 (자동 입력) */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /** 삭제 시각 (Soft Delete용) */
    @Column
    private Instant deletedAt;

    /** 모집 포지션 리스트 (1:N) */
    @OneToMany(
            mappedBy = "projectRecruitment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProjectRecruitmentPosition> positions= new ArrayList<>();

    /** 모집 성향 리스트 (1:N) */
    @OneToMany(
            mappedBy = "projectRecruitment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProjectRecruitmentTrait> traits= new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (title != null) title = title.trim();
        if (bannerImageUrl != null) bannerImageUrl = bannerImageUrl.trim();
        if (roadName != null) roadName = roadName.trim();
        if (region1depthName != null) region1depthName = region1depthName.trim();
        if (region2depthName != null) region2depthName = region2depthName.trim();
        if (region3depthName != null) region3depthName = region3depthName.trim();
        if (mainBuildingNo != null) mainBuildingNo = mainBuildingNo.trim();
        if (subBuildingNo != null) subBuildingNo = subBuildingNo.trim();
        if (zoneNo != null) zoneNo = zoneNo.trim();
    }

    /** OFFLINE일 때 응답용 전체 도로명 주소 조립 */
    public String fullRoadAddress() {
        if (projectMode == ProjectMode.ONLINE) return "ONLINE";
        if (roadName == null && region1depthName == null && region2depthName == null) return "-";

        StringBuilder sb = new StringBuilder();
        if (region1depthName != null) sb.append(region1depthName).append(" ");
        if (region2depthName != null) sb.append(region2depthName).append(" ");
        if (region3depthName != null) sb.append(region3depthName).append(" ");
        if (roadName != null) sb.append(roadName).append(" ");

        String main = Objects.toString(mainBuildingNo, "").trim();
        String sub  = Objects.toString(subBuildingNo, "").trim();
        if (!main.isEmpty() && !sub.isEmpty()) sb.append(main).append("-").append(sub);
        else if (!main.isEmpty()) sb.append(main);

        return sb.toString().trim();
    }


}
