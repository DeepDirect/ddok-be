package goorm.ddok.chat.service;

import goorm.ddok.chat.domain.ChatRoomMember;
import goorm.ddok.chat.dto.response.ChatMessageResponse;
import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMemberRepository memberRepo;

    public void notifyNewMessage(ChatMessageResponse saved) {

        Long roomId = saved.getRoomId();
        Long senderId = saved.getSenderId();
        Instant createdAt = saved.getCreatedAt();

        // 멤버 조회
        List<ChatRoomMember> members =
                memberRepo.findActiveMembersExcludingSender(roomId, senderId == null ? -1L : senderId);

        for (ChatRoomMember m : members) {
            Long userId = m.getUser().getId();

            Map<String, Object> payload = Map.of(
                    "type", "NEW_MESSAGE_FLAG",
                    "roomId", roomId,
                    "createdAt", createdAt
            );

            String destination = "/sub/users/" + userId + "/notifications";
            messagingTemplate.convertAndSend(destination, payload);
        }
    }


}
