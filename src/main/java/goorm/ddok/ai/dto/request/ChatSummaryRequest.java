package goorm.ddok.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSummaryRequest {
    @NotNull private Long fromMessageId;
    @NotNull private Long toMessageId;
    private Integer maxTokens; // optional
}