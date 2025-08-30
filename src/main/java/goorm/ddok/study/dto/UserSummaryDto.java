package goorm.ddok.study.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// ★ null이어도 키가 항상 나오도록
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(name = "StudyUserSummary")
public class UserSummaryDto {

    @Schema(description = "사용자 ID", example = "101")
    private Long userId;

    @Schema(description = "닉네임", example = "개구라")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "대표 포지션", example = "백엔드")
    private String mainPosition;

    @Schema(description = "메인 배지")
    private BadgeDto mainBadge;         // null 가능, 키는 유지

    @Schema(description = "탈주 배지")
    private AbandonBadgeDto abandonBadge; // null 가능, 키는 유지

    @Schema(description = "온도(없으면 null)", example = "36.5")
    private BigDecimal temperature;     // null 가능, 키는 유지

    @JsonProperty("isMine")
    @Schema(description = "현재 사용자 본인 여부", example = "true")
    private boolean IsMine;

    @Schema(description = "채팅방 ID", example = "null")
    private Long chatRoomId;

    @JsonProperty("dmRequestPending")
    @Schema(description = "DM 요청 대기 여부", example = "false")
    private boolean dmRequestPending;
}