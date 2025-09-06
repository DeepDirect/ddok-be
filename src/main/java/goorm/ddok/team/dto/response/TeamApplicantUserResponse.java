package goorm.ddok.team.dto.response;

import goorm.ddok.badge.domain.BadgeType;
import goorm.ddok.badge.domain.UserBadge;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Comparator;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamApplicantUserResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private BigDecimal temperature;
    private String mainPosition;
    private Long chatRoomId;
    private boolean dmRequestPending;
    private BadgeDto mainBadge;
    private AbandonBadgeDto abandonBadge;

}
