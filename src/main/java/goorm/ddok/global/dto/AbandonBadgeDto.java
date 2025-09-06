package goorm.ddok.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "탈주 배지 정보 DTO")
public class AbandonBadgeDto {

    @Schema(description = "배지 보유 여부", example = "true")
    private boolean IsGranted;

    @Schema(description = "포기 횟수", example = "5")
    private Integer count;

    public static AbandonBadgeDto from(UserBadge badge) {
        if (badge == null) {
            return AbandonBadgeDto.builder()
                    .IsGranted(false)
                    .count(0)
                    .build();
        }
        return AbandonBadgeDto.builder()
                .IsGranted(badge.getTotalCnt() > 0)
                .count(badge.getTotalCnt())
                .build();
    }
}
