package goorm.ddok.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "배지 정보 DTO")
public class BadgeDto {
    // TODO: 추후 배지 타입/티어 ENUM 정의 시 타입 변경

    @Schema(description = "배지 타입", example = "login")
    private String type;

    @Schema(description = "배지 티어", example = "bronze")
    private String tier;

}