package goorm.ddok.global.dto;

import goorm.ddok.badge.domain.BadgeTier;
import goorm.ddok.badge.domain.BadgeType;
import goorm.ddok.badge.domain.UserBadge;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "배지 정보 DTO")
public class BadgeDto {

    @Schema(description = "배지 타입", example = "login")
    private BadgeType type;

    @Schema(description = "배지 티어", example = "bronze")
    private BadgeTier tier;

    public static BadgeDto from(UserBadge badge, BadgeTier tier) {
        if (badge == null) return null;
        return BadgeDto.builder()
                .type(badge.getBadgeType())
                .tier(tier)
                .build();
    }

}