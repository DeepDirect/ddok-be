// goorm/ddok/study/dto/response/StudyUpdateResultResponse.java
package goorm.ddok.study.dto.response;

import goorm.ddok.global.dto.PreferredAgesDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StudyUpdateResultResponse {

    private Long studyId;
    private boolean IsMine;

    private String title;
    private String teamStatus;
    private String bannerImageUrl;

    private List<String> traits;
    private Integer capacity;
    private Long applicantCount;

    private String mode;               // "ONLINE" | "OFFLINE"
    private String address;            // OFFLINE: 합성 주소 / ONLINE: "ONLINE"

    private PreferredAgesDto preferredAges;

    private Integer expectedMonth;
    private LocalDate startDate;

    private String studyType;
    private String detail;
}