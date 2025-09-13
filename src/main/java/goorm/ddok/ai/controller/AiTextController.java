package goorm.ddok.ai.controller;

import goorm.ddok.ai.dto.request.AiProjectRequest;
import goorm.ddok.ai.dto.request.AiStudyRequest;
import goorm.ddok.ai.dto.response.AiTextResponse;
import goorm.ddok.ai.service.AiTextService;
import goorm.ddok.global.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiTextController {

    private final AiTextService ai;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/projects/ai")
    public ResponseEntity<ApiResponseDto<AiTextResponse>> projectAi(@RequestBody AiProjectRequest req) {
        String detail = ai.generateProjectDetail(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.of(201, "상세 내용 생성이 성공했습니다.", new AiTextResponse(detail)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/studies/ai")
    public ResponseEntity<ApiResponseDto<AiTextResponse>> studyAi(@RequestBody AiStudyRequest req) {
        String detail = ai.generateStudyDetail(req);
        return ResponseEntity.ok(ApiResponseDto.of(200, "요청이 성공적으로 처리되었습니다.", new AiTextResponse(detail)));
    }
}