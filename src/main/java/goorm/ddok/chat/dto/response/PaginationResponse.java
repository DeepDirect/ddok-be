package goorm.ddok.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이징 정보")
public class PaginationResponse {

    @Schema(description = "현재 페이지")
    private Integer currentPage;

    @Schema(description = "페이지 크기")
    private Integer pageSize;

    @Schema(description = "전체 페이지 수")
    private Integer totalPages;

    @Schema(description = "전체 항목 수")
    private Long totalItems;

    public static PaginationResponse of(Page<?> page) {
        return PaginationResponse.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .build();
    }
}
