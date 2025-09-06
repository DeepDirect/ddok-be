package goorm.ddok.project.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "project_application",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_application_applicant_position",
                columnNames = {"applicant_id", "position_id"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProjectApplication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    /** 지원 대상 공고 (N:1) */
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "project_id", nullable = false)
//    private ProjectRecruitment projectRecruitment;

    /** 지원자 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User user;

    /** 지원 포지션 (N:1) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private ProjectRecruitmentPosition position;

    /** 지원 상태 (대기/승인/거절) */
    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false)
    private ApplicationStatus status;

    /** 생성 시각 */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 최종 수정 시각 (상태 변경 시 갱신) */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public void approve() {
        this.status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        this.status = ApplicationStatus.REJECTED;
    }

    public boolean isPending() {
        return this.status == ApplicationStatus.PENDING;
    }
}