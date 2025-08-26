package goorm.ddok.study.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UserSummaryDto", description = "사용자 요약 정보 DTO")
public class UserSummaryDto {

    @Schema(description = "사용자 ID", example = "101")
    private Long userId;

    @Schema(description = "닉네임", example = "개구라")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/images/user101.png")
    private String profileImageUrl;

    @Schema(description = "주 포지션", example = "풀스택")
    private String mainPosition;

    @Schema(description = "온도", example = "36.5")
    private Double temperature;

    @Schema(description = "내 프로필 여부", example = "false")
    @JsonProperty("isMine")
    private Boolean isMine;

    @Schema(description = "채팅방 ID (없으면 null)", example = "null")
    private Long chatRoomId;

    @Schema(description = "DM 요청 대기 여부", example = "true")
    private Boolean dmRequestPending;
}