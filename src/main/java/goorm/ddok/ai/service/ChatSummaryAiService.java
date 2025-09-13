package goorm.ddok.ai.service;

import goorm.ddok.ai.dto.request.ChatSummaryRequest;
import goorm.ddok.ai.dto.response.ChatSummaryResponse;
import goorm.ddok.ai.service.prompt.ChatSummaryPromptFactory;
import goorm.ddok.ai.service.provider.AiModelClient;
import goorm.ddok.chat.domain.ChatContentType;
import goorm.ddok.chat.domain.ChatMessage;
import goorm.ddok.chat.domain.ChatRoom;
import goorm.ddok.chat.repository.ChatMessageRepository;
import goorm.ddok.chat.repository.ChatRepository;
import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSummaryAiService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRepository chatRepository;
    private final AiModelClient aiModelClient;

    private static final int MAX_MESSAGES = 500;
    private static final int DEFAULT_MAX_TOKENS = 700;

    public ChatSummaryResponse summarize(Long roomId, CustomUserDetails me, ChatSummaryRequest req) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);
        if (req.getFromMessageId() == null || req.getToMessageId() == null
                || req.getFromMessageId() > req.getToMessageId()) {
            throw new GlobalException(ErrorCode.INVALID_CHAT_RANGE);
        }

        // 1) 방 존재 확인 (User 등 다른 연관 엔티티는 접근하지 않음)
        ChatRoom room = chatRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2) 멤버십 확인: id 기반으로만 검사 (User 엔티티 로딩 금지)
        boolean member = chatRoomMemberRepository
                .existsByRoom_IdAndUser_IdAndDeletedAtIsNull(roomId, me.getId());
        if (!member) throw new GlobalException(ErrorCode.NOT_CHAT_MEMBER);

        // 3) 메시지 범위 로딩 (소프트 삭제 제외, id 오름차순)
        // ⬇️ 이 메서드는 ChatMessageRepository에 추가되어 있어야 합니다.
        List<ChatMessage> range = chatMessageRepository
                .findByRoom_IdAndIdBetweenAndDeletedAtIsNullOrderByIdAsc(
                        roomId, req.getFromMessageId(), req.getToMessageId()
                );

        if (range.isEmpty()) throw new GlobalException(ErrorCode.CHAT_MESSAGE_INVALID);

        // TEXT만 필터, 내용 빈 것 제외, 최대 N개
        List<ChatMessage> texts = range.stream()
                .filter(m -> m.getContentType() == null || m.getContentType() == ChatContentType.TEXT)
                .filter(m -> m.getContentText() != null && !m.getContentText().isBlank())
                .sorted(Comparator.comparing(ChatMessage::getId)) // 안전하게 정렬 보장
                .limit(MAX_MESSAGES)
                .toList();

        if (texts.isEmpty()) throw new GlobalException(ErrorCode.NOT_FOUND);

        // 4) 프롬프트 생성
        String roomTitle = (room.getName() == null || room.getName().isBlank()) ? "채팅방" : room.getName();
        String prompt = ChatSummaryPromptFactory.build(roomTitle, texts);

        // 5) 호출
        int maxTokens = (req.getMaxTokens() == null || req.getMaxTokens() <= 0)
                ? DEFAULT_MAX_TOKENS
                : req.getMaxTokens();

        String summary = aiModelClient.generate(prompt, maxTokens);

        // 6) 응답
        return ChatSummaryResponse.builder()
                .roomId(roomId)
                .fromMessageId(req.getFromMessageId())
                .toMessageId(req.getToMessageId())
                .usedMessages(texts.size())
                .summary(summary)
                .build();
    }
}