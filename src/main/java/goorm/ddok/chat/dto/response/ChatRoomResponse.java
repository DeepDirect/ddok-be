package goorm.ddok.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import goorm.ddok.chat.domain.ChatMemberRole;
import goorm.ddok.chat.domain.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 정보")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private Long roomId;

    @Schema(description = "채팅방 타입", example = "PRIVATE")
    private ChatRoomType roomType;

    @Schema(description = "채팅방 이름", example = "채팅방이에옹")
    private String name;

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "고정 여부", example = "false")
    private Boolean isPinned;

    @Schema(description = "멤버 수 (그룹 채팅만)", example = "3")
    private Integer memberCount;

    @Schema(description = "내 역할 (그룹 채팅만)", example = "MEMBER")
    private ChatMemberRole myRole;

    @Schema(description = "방장 정보 (그룹 채팅만)")
    private UserSimpleResponse owner;

    @Schema(description = "상대방 정보 (개인 채팅만)")
    private OtherUserResponse otherUser;

    @Schema(description = "마지막 메시지")
    private LastMessageResponse lastMessage;

    @Schema(description = "안 읽은 메시지 존재 여부", example = "true")
    private Boolean hasUnreadMessages;

    @Schema(description = "마지막 업데이트 시간")
    private Instant updatedAt;
}
