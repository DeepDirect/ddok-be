package goorm.ddok.global.websocket;

import goorm.ddok.chat.repository.ChatRoomMemberRepository;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private static final Pattern SUB_ROOM_PATTERN = Pattern.compile("^/sub/chats/(\\d+)$");
    private static final Pattern PUB_ROOM_PATTERN = Pattern.compile("^/pub/chats/(\\d+)(?:/.*)?$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
        StompCommand cmd = acc.getCommand();
        if (cmd == null) return message;

        switch (cmd) {
            case CONNECT -> {
                String authHeader = acc.getFirstNativeHeader("Authorization");
                if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                    throw new GlobalException(ErrorCode.UNAUTHORIZED);
                }
                String token = authHeader.substring(7);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId == null) {
                    throw new GlobalException(ErrorCode.UNAUTHORIZED);
                }
                acc.getSessionAttributes().put("userId", userId);
                acc.setUser(new StompPrincipal(String.valueOf(userId)));
            }
            case SUBSCRIBE -> {
                Long userId = (Long) acc.getSessionAttributes().get("userId");
                if (userId == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

                String dest = acc.getDestination();
                if (!StringUtils.hasText(dest)) break;

                if (dest.startsWith("/user/")) break;

                Long roomId = extractId(dest, SUB_ROOM_PATTERN);
                if (roomId != null && !isActiveMember(roomId, userId)) {
                    throw new GlobalException(ErrorCode.FORBIDDEN);
                }
            }
            case SEND -> {
                Long userId = (Long) acc.getSessionAttributes().get("userId");
                if (userId == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

                String dest = acc.getDestination();
                Long roomId = extractId(dest, PUB_ROOM_PATTERN);
                if (roomId != null && !isActiveMember(roomId, userId)) {
                    throw new GlobalException(ErrorCode.FORBIDDEN);
                }
            }
            default -> { }
        }

        return message;
    }

    private Long extractId(String destination, Pattern pattern) {
        if (!StringUtils.hasText(destination)) return null;
        Matcher m = pattern.matcher(destination);
        if (m.matches()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private boolean isActiveMember(Long roomId, Long userId) {
        return chatRoomMemberRepository.existsByRoom_IdAndUser_IdAndDeletedAtIsNull(roomId, userId);
    }
}
