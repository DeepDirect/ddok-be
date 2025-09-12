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
import goorm.ddok.member.domain.User;
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

    public ChatSummaryResponse summarize(Long roomId, User me, ChatSummaryRequest req) {
        if (me == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);
        if (req.getFromMessageId() > req.getToMessageId()) {
            throw new GlobalException(ErrorCode.INVALID_CHAT_RANGE);
        }

        // 방 멤버 권한 확인
        boolean member = chatRoomMemberRepository
                .existsByRoom_IdAndUser_IdAndDeletedAtIsNull(roomId, me.getId());
        if (!member) throw new GlobalException(ErrorCode.NOT_CHAT_MEMBER);

        // 메시지 범위 로드
        List<ChatMessage> all = chatMessageRepository
                .findByRoom_IdAndIdBetweenOrderById(roomId, req.getFromMessageId(), req.getToMessageId());
        if (all.isEmpty()) throw new GlobalException(ErrorCode.CHAT_MESSAGE_INVALID);

        // TEXT만, id 오름차순, 최대 N개
        List<ChatMessage> texts = all.stream()
                .filter(m -> m.getContentType() == null || m.getContentType() == ChatContentType.TEXT)
                .sorted(Comparator.comparing(ChatMessage::getId))
                .limit(MAX_MESSAGES)
                .toList();
        if (texts.isEmpty()) throw new GlobalException(ErrorCode.NOT_FOUND);

        String roomTitle = chatRepository.findById(roomId)
                .map(ChatRoom::getName)
                .orElse("채팅방");

        String prompt = ChatSummaryPromptFactory.build(roomTitle, texts);

        int maxTokens = (req.getMaxTokens() == null || req.getMaxTokens() <= 0)
                ? DEFAULT_MAX_TOKENS : req.getMaxTokens();

        String summary = aiModelClient.generate(prompt, maxTokens);

        return ChatSummaryResponse.builder()
                .roomId(roomId)
                .fromMessageId(req.getFromMessageId())
                .toMessageId(req.getToMessageId())
                .usedMessages(texts.size())
                .summary(summary)
                .build();
    }
}