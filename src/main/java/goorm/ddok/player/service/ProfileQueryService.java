package goorm.ddok.player.service;

import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.domain.*;
import goorm.ddok.member.dto.response.ActiveHoursResponse;
import goorm.ddok.member.dto.response.LocationResponse;
import goorm.ddok.member.repository.UserPortfolioRepository;
import goorm.ddok.member.repository.UserPositionRepository;
import goorm.ddok.member.repository.UserRepository;
import goorm.ddok.member.repository.UserTraitRepository;
import goorm.ddok.player.dto.response.ProfileDetailResponse;
import goorm.ddok.player.dto.response.UserPortfolioResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProfileQueryService.class);
    private final UserRepository userRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final UserTraitRepository userTraitRepository;
    private final UserPositionRepository userPositionRepository;

    public ProfileDetailResponse getProfile(Long targetUserId, Long loginUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        boolean isMine = targetUserId.equals(loginUserId);
        log.info("🔑 targetUserId={}, loginUserId={}", targetUserId, loginUserId);


        // 프로필 공개 여부 확인
        if (!isMine && !user.isPublic()) {
            return ProfileDetailResponse.builder()
                    .userId(user.getId())
                    .IsMine(isMine)
                    .IsPublic(user.isPublic())
                    .profileImageUrl(user.getProfileImageUrl())
                    .nickname(user.getNickname())
                    .temperature(findTemperature(user))
                    .ageGroup(user.getAgeGroup())
                    .mainBadge(toBadgeDto(user))
                    .content(user.getIntroduce())
                    .build();
        }

        // 공개 상태 -> 전체 데이터 조립
        return ProfileDetailResponse.builder()
                .userId(user.getId())
                .IsMine(isMine)
                .IsPublic(user.isPublic())
                .chatRoomId(null) // TODO: DM 기능 구현 시 교체
                .dmRequestPending(false) // TODO: DM 요청 여부 확인 로직 추가
                .profileImageUrl(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .temperature(findTemperature(user))
                .ageGroup(user.getAgeGroup())
                .mainPosition(findMainPosition(user))
                .subPositions(findSubPositions(user))
                .mainBadge(toBadgeDto(user))
                .abandonBadge(toAbandonBadgeDto(user))
                .activeHours(toActiveHours(user.getActivity()))
                .traits(findTraits(user))
                .content(user.getIntroduce())
                .portfolio(findPortfolio(user))
                .location(toLocation(user.getLocation()))
                .build();
    }

    // TODO: 온도 구현 시 교체
    private BigDecimal findTemperature(User user) {
        if (user.getReputation() == null || user.getReputation().getTemperature() == null) {
            return BigDecimal.valueOf(36.5);
//            throw new GlobalException(ErrorCode.REPUTATION_NOT_FOUND);
        }
        return user.getReputation().getTemperature();
    }

    private String findMainPosition(User user) {
        return user.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_POSITION_NOT_FOUND));
    }

    private List<String> findSubPositions(User user) {
        return user.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.SECONDARY)
                .map(UserPosition::getPositionName)
                .toList();
    }

    private List<String> findTraits(User user) {
        return user.getTraits().stream()
                .map(UserTrait::getTraitName)
                .toList();
    }

    private List<UserPortfolioResponse> findPortfolio(User user) {
        return userPortfolioRepository.findByUser(user).stream()
                .map(p -> new UserPortfolioResponse(p.getLinkTitle(), p.getLink()))
                .toList();
    }

    private ActiveHoursResponse toActiveHours(UserActivity activity) {
        if (activity == null) return null;
        return new ActiveHoursResponse(
                String.valueOf(activity.getActivityStartTime()),
                String.valueOf(activity.getActivityEndTime())
        );
    }

    private LocationResponse toLocation(UserLocation location) {
        if (location == null) return null;
        return new LocationResponse(
                location.getActivityLatitude(),
                location.getActivityLongitude(),
                location.getRoadName()
        );
    }

    private BadgeDto toBadgeDto(User user) {
        // TODO: 대표 뱃지 조회 로직 구현
        return new BadgeDto("login", "bronze");
    }

    private AbandonBadgeDto toAbandonBadgeDto(User user) {
        // TODO: 탈주 배지 조회 로직 구현
        return new AbandonBadgeDto(true, 5);
    }
}