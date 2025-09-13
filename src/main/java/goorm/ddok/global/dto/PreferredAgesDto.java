package goorm.ddok.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "PreferredAgesDto", description = "선호 연령대 (무관 시 null)")
public class PreferredAgesDto {

    @Schema(description = "최소 연령 (무관 시 null)", example = "20")
    private Integer ageMin;

    @Schema(description = "최대 연령 (무관 시 null)", example = "30")
    private Integer ageMax;

    public static PreferredAgesDto of(Integer min, Integer max) {
        if (min == null && max == null) return null;
        return PreferredAgesDto.builder()
                .ageMin(min)
                .ageMax(max)
                .build();
    }
}