package goorm.ddok.ai.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSummaryResponse {
    private Long roomId;
    private Long fromMessageId;
    private Long toMessageId;
    private Integer usedMessages;
    private String summary; // 마크다운
}