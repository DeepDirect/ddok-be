// goorm/ddok/ai/controller/AiChatSummaryController.java
package goorm.ddok.ai.controller;

import goorm.ddok.ai.dto.request.ChatSummaryRequest;
import goorm.ddok.ai.dto.response.ChatSummaryResponse;
import goorm.ddok.ai.service.ChatSummaryAiService;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.member.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chat") // <= 여기 prefix 확인
public class AiChatSummaryController {

    private final ChatSummaryAiService chatSummaryService;

    @PostMapping("/rooms/{roomId}/summary")
    public ResponseEntity<ChatSummaryResponse> summarize(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChatSummaryRequest req
    ) {
        return ResponseEntity.ok(chatSummaryService.summarize(roomId, user,req));
    }
}