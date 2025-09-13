package goorm.ddok.ai.dto.request;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiProjectRequest {
    private String title;
    private LocalDate expectedStart;
    private Integer expectedMonth;
    private String mode; // "online" / "offline"
    private LocationDto location;
    private PreferredAgesDto preferredAges;
    private Integer capacity;
    private List<String> traits;
    private List<String> positions;
    private String leaderPosition;
    private String detail; // 사용자가 이미 쓴 텍스트(있으면 tone 참고)
}