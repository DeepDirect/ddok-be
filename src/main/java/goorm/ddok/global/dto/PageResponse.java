package goorm.ddok.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "페이지네이션 응답 DTO")
public class PageResponse<T> {

    @Schema(description = "페이지네이션 정보")
    private Pagination pagination;

    @Schema(description = "조회 데이터 목록")
    private List<T> items;


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pagination {
        @Schema(description = "현재 페이지", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        @Schema(description = "전체 페이지 수", example = "1")
        private int totalPages;

        @Schema(description = "전체 아이템 수", example = "3")
        private long totalItems;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .pagination(Pagination.builder()
                        .currentPage(page.getNumber())
                        .pageSize(page.getSize()) // 요청한 사이즈 (예: 14)
                        .totalPages(page.getTotalPages())
                        .totalItems(page.getTotalElements())
                        .build())
                .items(page.getContent())
                .build();
    }
}