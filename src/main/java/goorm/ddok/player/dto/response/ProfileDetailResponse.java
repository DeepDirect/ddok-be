package goorm.ddok.player.dto.response;

import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.member.dto.response.ActiveHoursResponse;
import goorm.ddok.member.dto.response.LocationResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "ProfileDetailResponse",
        description = "프로필 상세 조회 응답 DTO"
)
public class ProfileDetailResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "내 프로필 여부", example = "true")
    private boolean IsMine;

    @Schema(description = "프로필 공개 여부", example = "true")
    private boolean IsPublic;

    @Schema(description = "채팅방 ID (없으면 null)", example = "null")
    private Long chatRoomId;

    @Schema(description = "DM 요청 대기 여부", example = "false")
    private boolean dmRequestPending;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.png")
    private String profileImageUrl;

    @Schema(description = "닉네임", example = "똑똑한 똑똑이")
    private String nickname;

    @Schema(description = "온도", example = "36.6")
    private BigDecimal temperature;

    @Schema(description = "나이대", example = "20대")
    private String ageGroup;

    @Schema(description = "메인 포지션", example = "backend")
    private String mainPosition;

    @Schema(description = "서브 포지션 목록", example = "[\"frontend\", \"devops\"]")
    private List<String> subPositions;

    @Schema(description = "대표 배지")
    private BadgeDto mainBadge;

    @Schema(description = "탈주 배지 정보")
    private AbandonBadgeDto abandonBadge;

    @Schema(description = "활동 시간대")
    private ActiveHoursResponse activeHours;

    @Schema(description = "사용자 특성", example = "[\"정리의 신\", \"실행력 갓\", \"내향인\"]")
    private List<String> traits;

    @Schema(description = "자기소개", example = "Hi there, ~")
    @Size(max = 130, message = "자기소개는 최대 130자까지 입력 가능합니다.")
    private String content;

    @Schema(description = "포트폴리오 링크")
    private List<UserPortfolioResponse> portfolio;

    @Schema(description = "위치 정보")
    private LocationResponse location;

}
