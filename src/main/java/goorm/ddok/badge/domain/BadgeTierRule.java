package goorm.ddok.badge.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 배지 티어 규칙 엔티티
 *
 * - 착한 배지(complete, leader_complete, login)에만 적용됨
 * - 누적 횟수(total_cnt)에 따라 티어(bronze, silver, gold)를 결정하기 위한 기준
 * - ex) complete: {0=bronze, 5=silver, 10=gold}
 */
@Entity
@Table(
        name = "badge_tier_rule",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"badge_type", "tier"}),
                @UniqueConstraint(columnNames = {"badge_type", "required_cnt"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BadgeTierRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false, length = 30)
    private BadgeType badgeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 30)
    private BadgeTier tier;

    @Column(name = "required_cnt", nullable = false)
    private Integer requiredCnt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
