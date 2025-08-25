package goorm.ddok.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "기술 스택 검색 응답 DTO")
public class TechStackResponse {

    @Schema(description = "검색된 기술 스택 리스트", example = "[\"SpringBoot\", \"SpringDataJPA\", \"SpringSecurity\"]")
    private List<String> techStacks;
}
