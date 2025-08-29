package goorm.ddok.player.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPortfolioResponse {
    @Schema(description = "링크 제목", example = "github link")
    private String linkTitle;

    @Schema(description = "링크 주소", example = "https://~")
    private String link;
}
