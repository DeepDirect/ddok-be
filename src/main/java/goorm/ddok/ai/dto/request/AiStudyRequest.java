package goorm.ddok.ai.dto.request;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiStudyRequest {
    private String title;
    private LocalDate expectedStart;
    private Integer expectedMonth;
    private String mode; // "online" / "offline"
    private LocationDto location;
    private PreferredAgesDto preferredAges;
    private Integer capacity;
    private List<String> traits;
    private String studyType; // 예: "ALGORITHM", "자소서" 등
    private String detail;
}