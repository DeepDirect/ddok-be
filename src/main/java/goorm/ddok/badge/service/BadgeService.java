package goorm.ddok.badge.service;

import goorm.ddok.badge.domain.BadgeTier;
import goorm.ddok.badge.domain.BadgeTierRule;
import goorm.ddok.badge.domain.BadgeType;
import goorm.ddok.badge.domain.UserBadge;
import goorm.ddok.badge.repository.BadgeTierRuleRepository;
import goorm.ddok.badge.repository.UserBadgeRepository;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.member.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BadgeService {
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeTierRuleRepository badgeTierRuleRepository;

    /**
     * 배지 카운트 증가
     */
    public UserBadge increaseBadge(User user, BadgeType badgeType) {
        UserBadge userBadge = userBadgeRepository.findByUserAndBadgeType(user, badgeType)
                .orElseGet(() -> UserBadge.create(user, badgeType));

        userBadge.increaseCount();

        return userBadgeRepository.save(userBadge);
    }

    /**
     * 착한 배지 조회
     */
    @Transactional(readOnly = true)
    public List<BadgeDto> getGoodBadges(User user) {
        List<UserBadge> badges = userBadgeRepository.findByUserAndBadgeTypeIn(
                user,
                List.of(BadgeType.complete, BadgeType.leader_complete, BadgeType.login)
        );

        return badges.stream()
                .map(this::toGoodBadgeDto)
                .toList();
    }

    /**
     * 나쁜 배지 조회
     */
    @Transactional(readOnly = true)
    public AbandonBadgeDto getAbandonBadge(User user) {
        return userBadgeRepository.findByUserAndBadgeType(user, BadgeType.abandon)
                .map(badge -> AbandonBadgeDto.builder()
                        .IsGranted(badge.getTotalCnt() > 0)
                        .count(badge.getTotalCnt())
                        .build())
                .orElse(AbandonBadgeDto.builder()
                        .IsGranted(false)
                        .count(0)
                        .build());
    }

    /**
     * 착한 배지 변환
     */
    private BadgeDto toGoodBadgeDto(UserBadge userBadge) {
        BadgeTier tier = calculateTier(userBadge.getBadgeType(), userBadge.getTotalCnt());
        return BadgeDto.builder()
                .type(userBadge.getBadgeType())
                .tier(tier)
                .build();
    }

    /**
     * 누적 카운트에 따른 티어 계산
     */
    private BadgeTier calculateTier(BadgeType type, int totalCnt) {
        return badgeTierRuleRepository
                .findTopByBadgeTypeAndRequiredCntLessThanEqualOrderByRequiredCntDesc(type, totalCnt)
                .map(BadgeTierRule::getTier)
                .orElse(BadgeTier.BRONZE);
    }


}
