package goorm.ddok.badge.repository;

import goorm.ddok.badge.domain.UserBadge;
import goorm.ddok.badge.domain.BadgeType;
import goorm.ddok.member.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 특정 유저 + 배지 타입 조회
    Optional<UserBadge> findByUserAndBadgeType(User user, BadgeType badgeType);

    // 특정 유저 + 여러 배지 타입 조회
    List<UserBadge> findByUserAndBadgeTypeIn(User user, List<BadgeType> badgeTypes);
}
