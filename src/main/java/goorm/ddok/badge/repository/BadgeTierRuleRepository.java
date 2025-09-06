package goorm.ddok.badge.repository;

import goorm.ddok.badge.domain.BadgeTierRule;
import goorm.ddok.badge.domain.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeTierRuleRepository extends JpaRepository<BadgeTierRule, Long> {

    // 누적 카운트 이하 중 가장 큰 requiredCnt를 가진 룰 조회
    Optional<BadgeTierRule> findTopByBadgeTypeAndRequiredCntLessThanEqualOrderByRequiredCntDesc(
            BadgeType badgeType,
            int requiredCnt
    );
}
