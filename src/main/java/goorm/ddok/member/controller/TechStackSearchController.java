package goorm.ddok.member.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.member.dto.response.TechStackResponse;
import goorm.ddok.member.service.TechStackSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Validated
public class TechStackSearchController {

    private final TechStackSearchService techStackSearchService;

    @Operation(
            summary = "기술 스택 검색",
            description = "키워드로 기술 스택을 검색합니다. (as-you-type + 오타 허용)",
            security = @SecurityRequirement(name = "Authorization")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "기술 스택 검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Spring 검색 결과",
                                    value = """
                                    {
                                      "status": 200,
                                      "message": "기술 스택 검색에 성공하였습니다.",
                                      "data": {
                                        "techStacks": [
                                          "Spring Boot",
                                          "Spring Data JPA",
                                          "Spring Security"
                                        ]
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/stacks")
    public ResponseEntity<ApiResponseDto<TechStackResponse>> searchTechStacks(
            @Parameter(description = "검색할 기술 스택 키워드(1~50자)", example = "spring")
            @RequestParam(required = false)
            @Size(min = 1, max = 50, message = "keyword는 1~50자여야 합니다.")
            String keyword
    ) {
        String q = keyword == null ? "" : keyword.strip();
        int len = q.codePointCount(0, q.length());  // 유니코드 안전 길이
        if (len < 1 || len > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.of(400, "keyword는 1~50자여야 합니다.", null));
        }

        TechStackResponse response = techStackSearchService.searchTechStacks(keyword);
        return ResponseEntity.ok(ApiResponseDto.of(200, "기술 스택 검색에 성공하였습니다.", response));
    }
}
