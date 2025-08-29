package goorm.ddok.chat.service;

import goorm.ddok.chat.domain.*;
import goorm.ddok.chat.dto.request.ChatMessageRequest;
import goorm.ddok.chat.dto.request.LastReadMessageRequest;
import goorm.ddok.chat.dto.response.*;
import goorm.ddok.chat.repository.ChatMessageRepository;
import goorm.ddok.chat.repository.ChatRepository;
import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import goorm.ddok.chat.util.ChatMapper;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatRoom getRoomById(Long roomId) {
        return chatRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    // 1:1 채팅 목록 조회
    public ChatListResponse getPrivateChats(String email, Pageable pageable) {

        User me = getUserByEmail(email);

        Page<ChatRoom> page = chatRepository.pageRoomsByMemberAndTypeOrderByRecent(
                me, ChatRoomType.PRIVATE, pageable);

        List<ChatRoomResponse> chats = chatMapper.toChatRoomDtoList(page.getContent(), me.getId());

        return ChatListResponse.builder()
                .chats(chats)
                .pagination(PaginationResponse.of(page))
                .build();
    }

    // 팀 채팅 목록 조회
    public ChatListResponse getTeamChats(String email, Pageable pageable) {

        User me = getUserByEmail(email);

        Page<ChatRoom> page = chatRepository.pageRoomsByMemberAndTypeOrderByRecent(
                me, ChatRoomType.GROUP, pageable);

        List<ChatRoomResponse> chats = chatMapper.toChatRoomDtoList(page.getContent(), me.getId());

        return ChatListResponse.builder()
                .chats(chats)
                .pagination(PaginationResponse.of(page))
                .build();
    }

    // 1:1 채팅 목록 검색
    public ChatListResponse searchPrivateChats(String email, String search, Pageable pageable) {

        User me = getUserByEmail(email);

        Page<ChatRoom> page = chatRepository.pagePrivateRoomsByMemberAndPeerNickname(
                me, search, pageable);

        List<ChatRoomResponse> chats = chatMapper.toChatRoomDtoList(page.getContent(), me.getId());
        return ChatListResponse.builder()
                .chats(chats)
                .pagination(PaginationResponse.of(page))
                .build();
    }

    // 팀 채팅 목록 검색
    public ChatListResponse searchTeamChats(String email, String search, Pageable pageable) {

        User me = getUserByEmail(email);

        Page<ChatRoom> page = chatRepository.pageGroupRoomsByMemberAndRoomOrMemberName(
                me, search, pageable);

        List<ChatRoomResponse> chats = chatMapper.toChatRoomDtoList(page.getContent(), me.getId());
        return ChatListResponse.builder()
                .chats(chats)
                .pagination(PaginationResponse.of(page))
                .build();
    }

    // 채팅 멤버 조회
    public ChatMembersResponse getRoomMembers(Long roomId, String email) {
        User me = getUserByEmail(email);
        ChatRoom roomRef = chatRepository.getReferenceById(roomId);

        boolean isMember = chatRoomMemberRepository.existsByRoomAndUserAndDeletedAtIsNull(roomRef, me);
        if (!isMember) throw new GlobalException(ErrorCode.NOT_CHAT_MEMBER);

        // User를 한 번에 가져오도록 EntityGraph/Fetch Join 사용
        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomWithUser(roomRef);
        List<ChatMembersResponse.Member> dtos = members.stream()
                .map(m -> ChatMembersResponse.Member.builder()
                        .userId(m.getUser().getId())
                        .nickname(m.getUser().getNickname())
                        .profileImage(m.getUser().getProfileImageUrl())
                        .role(String.valueOf(m.getRole()))
                        .joinedAt(m.getCreatedAt())
                        .build())
                .toList();

        return ChatMembersResponse.builder()
                .members(dtos)
                .totalCount(dtos.size())
                .build();
    }

    // 메세지 전송
    @Transactional
    public ChatMessageResponse sendMessage(String email, Long roomId, ChatMessageRequest request) {
        User sender = getUserByEmail(email);
        ChatRoom room = getRoomById(roomId);

        if (!chatRoomMemberRepository.existsByRoomAndUser(room, sender)) {
            throw new GlobalException(ErrorCode.NOT_CHAT_MEMBER);
        }

        ChatMessage replyTo = (request.getReplyToId() == null) ? null :
                chatMessageRepository.findById(request.getReplyToId()).orElse(null);

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .room(room)
                .sender(sender)
                .contentType(request.getContentType())
                .contentText(request.getContentText())
                .fileUrl(request.getFileUrl())
                .replyTo(replyTo)
                .build());

        room.setLastMessageAt(saved.getCreatedAt());

        return ChatMessageResponse.builder()
                .messageId(saved.getId())
                .roomId(room.getId())
                .senderId(sender.getId())
                .senderNickname(sender.getNickname())
                .contentType(saved.getContentType())
                .contentText(saved.getContentText())
                .fileUrl(saved.getFileUrl())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // 채팅 메세지 메시지 목록/검색
    @Transactional
    public ChatMessageListResponse getChatMessages(String email, Long roomId, Pageable pageable, String search) {
        User me = getUserByEmail(email);
        ChatRoom room = getRoomById(roomId);

        if (!chatRoomMemberRepository.existsByRoomAndUserAndDeletedAtIsNull(room, me)) {
            throw new GlobalException(ErrorCode.NOT_CHAT_MEMBER);
        }

        Page<ChatMessageRepository.MessageView> page =
                (search == null || search.isBlank())
                        ? chatMessageRepository.pageViewsByRoom(room, pageable)
                        : chatMessageRepository.pageViewsByRoomAndKeyword(room, search, pageable);

        List<ChatMessageResponse> messages = page.getContent().stream()
                .map(v -> ChatMessageResponse.builder()
                        .messageId(v.getId())
                        .roomId(room.getId())
                        .senderId(v.getSenderId())
                        .senderNickname(v.getSenderNickname())
                        .contentType(v.getContentType())
                        .contentText(v.getContentText())
                        .fileUrl(v.getFileUrl())
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();

        return ChatMessageListResponse.builder()
                .messages(messages)
                .pagination(PaginationResponse.of(page))
                .build();
    }

    // 마지막 읽은 메세지 처리
    @Transactional
    public void lastReadMessage(String email, Long roomId, LastReadMessageRequest request) {

        User me = getUserByEmail(email);
        ChatRoom roomRef = chatRepository.getReferenceById(roomId);

        ChatRoomMember m = chatRoomMemberRepository.findByRoomAndUser(roomRef, me)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_CHAT_MEMBER));

        m.setLastReadMessageId(request.getMessageId());
    }

    @Transactional
    public void createPrivateChatRoom(User sender, User receiver) {
        if (chatRepository.existsPrivateRoomByUserIds(sender.getId(), receiver.getId())) {
            throw new GlobalException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom room = chatRepository.save(ChatRoom.builder()
                .roomType(ChatRoomType.PRIVATE)
                .owner(sender)
                .build());

        ChatRoomMember admin = ChatRoomMember.builder()
                .room(room).user(sender).role(ChatMemberRole.ADMIN).build();
        ChatRoomMember member = ChatRoomMember.builder()
                .room(room).user(receiver).role(ChatMemberRole.MEMBER).build();

        chatRoomMemberRepository.saveAll(List.of(admin, member));
    }
}
