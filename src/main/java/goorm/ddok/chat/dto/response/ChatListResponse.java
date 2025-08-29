package goorm.ddok.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ㅊㅐ팅 목록 응답")
public class ChatListResponse {

    @Schema(description = "채팅방 목록")
    private List<ChatRoomResponse> chats;

    @Schema(description = "페이징 정보")
    private PaginationResponse pagination;
}
