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
@Schema(name = "TechStackResponse", description = "기술 스택 조회 응답 DTO")
public class TechStackResponse {

    @Schema(description = "기술 스택명", example = "SpringBoot")
    private String techStack;
}
