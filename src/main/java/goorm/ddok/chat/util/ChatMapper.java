package goorm.ddok.chat.util;

import goorm.ddok.chat.domain.ChatMessage;
import goorm.ddok.chat.domain.ChatRoom;
import goorm.ddok.chat.domain.ChatRoomMember;
import goorm.ddok.chat.domain.ChatRoomType;
import goorm.ddok.chat.dto.response.ChatRoomResponse;
import goorm.ddok.chat.dto.response.LastMessageResponse;
import goorm.ddok.chat.dto.response.OtherUserResponse;
import goorm.ddok.chat.dto.response.UserSimpleResponse;
import goorm.ddok.chat.repository.ChatRepository;
import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.repository.UserRepository;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMapper {
    private final ChatRepository chatRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final UserReputationRepository userReputationRepository;

    public List<ChatRoomResponse> toChatRoomDtoList(List<ChatRoom> chatRooms, Long currentUserId) {
        return chatRooms.stream()
                .map(chatRoom -> toChatRoomDto(chatRoom, currentUserId))
                .collect(Collectors.toList());
    }

    // ChatRoom을 ChatRoomResponse로 변환
    public ChatRoomResponse toChatRoomDto(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomResponse.ChatRoomResponseBuilder builder = ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomType(chatRoom.getRoomType())
                .hasUnreadMessages(chatRepository.hasUnreadByTime(chatRoom.getId(), currentUserId))
                .isPinned(false) // TODO: 고정 기능 구현 시 실제 값으로 변경
                .updatedAt(Optional.ofNullable(chatRoom.getLastMessageAt())
                        .orElse(chatRoom.getCreatedAt()));

        if (chatRoom.getTeam() != null) {
            builder.teamId(chatRoom.getTeam().getId());
        }

        // 채팅방 타입에 따라 다른 정보 설정
        if (chatRoom.getRoomType() == ChatRoomType.PRIVATE) {
            setPrivateChatInfo(builder, chatRoom, currentUserId);
        } else {
            setGroupChatInfo(builder, chatRoom, currentUserId);
        }

        // 마지막 메시지 설정
        setLastMessage(builder, chatRoom);

        return builder.build();
    }

    // 개인 채팅 정보 설정
    private void setPrivateChatInfo(ChatRoomResponse.ChatRoomResponseBuilder builder,
                                    ChatRoom chatRoom, Long currentUserId) {
        try {
            User otherUser = chatRoomMemberRepository
                    .findFirstByRoom_IdAndDeletedAtIsNullAndUser_IdNotOrderByCreatedAtAsc(
                            chatRoom.getId(), currentUserId)
                    .map(ChatRoomMember::getUser)
                    .orElseThrow(() -> new GlobalException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            String roomName = (chatRoom.getName() != null && !chatRoom.getName().isBlank())
                    ? chatRoom.getName()
                    : getNickname(otherUser);

            builder.name(roomName)
                    .otherUser(OtherUserResponse.builder()
                            .id(otherUser.getId())
                            .nickname(getNickname(otherUser))
                            .profileImage(getProfileImageUrl(otherUser))
                            .temperature(getTemperature(otherUser))
                            .build());

        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            // 예기치 못한 예외는 로깅 후 기본 값으로 처리
            log.error("개인 채팅 정보 설정 중 오류 발생 - roomId: {}", chatRoom.getId(), e);
            builder.name(chatRoom.getName() != null ? chatRoom.getName() : "채팅방");
        }
    }

    private String getProfileImageUrl(User user) {
        try {
            return user.getProfileImageUrl();
        } catch (Exception e) {
            return null;
        }
    }

    private String getNickname(User user) {
        try {
            return user.getNickname();
        } catch (Exception e) {
            log.warn("닉네임 추출 실패 - userId: {}", user.getId());
            return "사용자" + user.getId();
        }
    }

    private BigDecimal getTemperature(User user) {
        return userReputationRepository.findByUserId(user.getId())
                .map(UserReputation::getTemperature)
                .orElse(BigDecimal.valueOf(36.5));
    }

    // 그룹 채팅 정보 설정
    private void setGroupChatInfo(ChatRoomResponse.ChatRoomResponseBuilder builder,
                                  ChatRoom chatRoom, Long currentUserId) {
        builder.name(chatRoom.getName());

        // 멤버 수 조회
        Long memberCount = chatRepository.countMembersByRoomId(chatRoom.getId());
        builder.memberCount(memberCount.intValue());

        // 방장 정보 설정
        if (chatRoom.getOwnerUserId() != null) {
            User owner = userRepository.findById(chatRoom.getOwnerUserId())
                    .orElse(null);

            if (owner != null) {
                builder.owner(UserSimpleResponse.builder()
                        .id(owner.getId())
                        .nickname(getNickname(owner))
                        .profileImage(getProfileImageUrl(owner))
                        .build());
            }
        }

        // 현재 사용자의 역할 조회
        Optional<ChatRoomMember> currentMember = chatRepository.findMembershipByRoomIdAndUserId(
                chatRoom.getId(), currentUserId);
        currentMember.ifPresent(member -> builder.myRole(member.getRole()));
    }

    // 마지막 메시지 정보 설정
    private void setLastMessage(ChatRoomResponse.ChatRoomResponseBuilder builder, ChatRoom chatRoom) {
        Optional<ChatMessage> lastMessage = chatRepository.findLastMessageByRoomId(chatRoom.getId());

        lastMessage.ifPresent(message -> {
            String content = getMessageContent(message);

            builder.lastMessage(LastMessageResponse.builder()
                    .messageId(message.getId())
                    .type(message.getContentType())
                    .content(content)
                    .createdAt(message.getCreatedAt())
                    .senderId(message.getSender().getId())
                    .build());
        });
    }

    // 메시지 내용 추출 (타입에 따라 다르게 처리)
    private String getMessageContent(ChatMessage message) {
        return switch (message.getContentType()) {
            case TEXT -> message.getContentText();
            case IMAGE -> "이미지";
            case FILE -> "파일";
            case SYSTEM -> message.getContentText();
        };
    }

    private Map<Long, User> getUserMap(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }
}
