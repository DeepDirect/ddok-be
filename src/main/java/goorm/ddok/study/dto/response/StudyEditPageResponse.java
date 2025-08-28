package goorm.ddok.study.dto.response;

import goorm.ddok.global.dto.PreferredAgesDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "StudyEditPageResponse")
public record StudyEditPageResponse(
        String title,
        String teamStatus,
        String bannerImageUrl,
        List<String> traits,
        Integer capacity,
        Long applicantCount,
        String mode,
        String address,
        PreferredAgesDto preferredAges,
        Integer expectedMonth,
        LocalDate startDate,
        String detail,
        String studyType
) {}