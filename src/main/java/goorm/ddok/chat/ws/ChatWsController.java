package goorm.ddok.chat.ws;

import goorm.ddok.chat.domain.ChatContentType;
import goorm.ddok.chat.domain.ChatRoomMember;
import goorm.ddok.chat.dto.request.ChatMessageRequest;
import goorm.ddok.chat.dto.response.ChatMessageResponse;
import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import goorm.ddok.chat.service.ChatMessageService;
import goorm.ddok.chat.service.ChatNotificationService;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatNotificationService chatNotificationService;

    /**
     * 클라 SEND: /pub/chats/{roomId}/send
     * 서버 SUB:  /sub/chats/{roomId}
     */
    @MessageMapping("/chats/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageRequest payload,
                     @Header("simpSessionAttributes") Map<String, Object> sessionAttrs) {

        Long userId = (Long) sessionAttrs.get("userId");
        if (userId == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (payload.getContentType() == null) {
            payload.setContentType(ChatContentType.TEXT);
        }

        ChatMessageResponse saved = chatMessageService.sendMessage(userId, roomId, payload);

        messagingTemplate.convertAndSend("/sub/chats/" + roomId, saved);

        chatNotificationService.notifyNewMessage(saved);
    }

    @MessageMapping("/chats/{roomId}/enter")
    public void enter(@DestinationVariable Long roomId,
                      @Payload ChatMessageRequest payload,
                      @Header("simpSessionAttributes") Map<String, Object> sessionAttrs) {

        Long userId = (Long) sessionAttrs.get("userId");
        if (userId == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        if (payload.getContentType() == null) {
            payload.setContentType(ChatContentType.TEXT);
        }
        if (payload.getContentText() == null) {
            payload.setContentText("입장했습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        ChatMessageResponse saved = chatMessageService.sendMessage(userId, roomId, payload);
        messagingTemplate.convertAndSend("/sub/chats/" + roomId, saved);
    }
}
