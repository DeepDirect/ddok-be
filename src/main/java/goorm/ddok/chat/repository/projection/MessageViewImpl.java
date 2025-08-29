package goorm.ddok.chat.repository.projection;

import goorm.ddok.chat.domain.ChatContentType;
import goorm.ddok.chat.repository.ChatMessageRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class MessageViewImpl implements ChatMessageRepository.MessageView {
    private Long id;
    private Long senderId;
    private String senderNickname;
    private ChatContentType contentType;
    private String contentText;
    private String fileUrl;
    private Instant createdAt;
}
