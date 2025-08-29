package goorm.ddok.player.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPortfolioRequest {
    @NotBlank
    @Size(max = 15)
    private String linkTitle;

    @NotBlank
    @Pattern(
            regexp = "^(http|https)://.*$",
            message = "유효한 URL을 입력해야 합니다."
    )
    private String link;
}
