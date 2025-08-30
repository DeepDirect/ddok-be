package goorm.ddok.study.dto.request;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.StudyType;
import goorm.ddok.study.domain.TeamStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StudyRecruitmentUpdateRequest {
    private String title;
    private StudyType studyType;
    private TeamStatus teamStatus;

    private LocalDate expectedStart;
    private Integer expectedMonth;

    private StudyMode mode;          // ONLINE | OFFLINE
    private LocationDto location;    // OFFLINE일 때 필요

    private PreferredAgesDto preferredAges; // null → 무관(0/0)
    private Integer capacity;

    private List<String> traits;
    private String detail;

    private String bannerImageUrl;
}