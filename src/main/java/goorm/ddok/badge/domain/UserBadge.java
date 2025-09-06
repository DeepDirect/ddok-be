package goorm.ddok.badge.domain;

import goorm.ddok.member.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 유저 배지 엔티티
 *
 * - 착한 배지: total_cnt 값에 따라 badge_tier_rule 저장
 * - 나쁜 배지(abandon): total_cnt 값만 의미 있음 (티어 없음)
 */
@Entity
@Table(name = "user_badge",
        indexes = {
                @Index(name = "idx_user_badge_user", columnList = "user_id"),
                @Index(name = "idx_user_badge_type", columnList = "badge_type"),
                @Index(name = "idx_user_badge_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 배지 유형 (complete, leader_complete, login, abandon) */
    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false, length = 30)
    private BadgeType badgeType;

    /** 완료/탈주 누적 횟수 */
    @Column(name = "total_cnt", nullable = false)
    private Integer totalCnt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // == 생성 팩토리 메서드 ==
    public static UserBadge create(User user, BadgeType badgeType) {
        UserBadge badge = new UserBadge();
        badge.user = user;
        badge.badgeType = badgeType;
        badge.totalCnt = 0;
        return badge;
    }

    // 누적 카운트 증가 메서드
    public void increaseCount() {
        this.totalCnt = (this.totalCnt == null ? 1 : this.totalCnt + 1);
    }

    // soft delete 처리
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

}
