package goorm.ddok.study.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.StudyType;
import goorm.ddok.study.domain.TeamStatus;
import goorm.ddok.study.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "StudyRecruitmentDetailResponse",
        description = "스터디 모집글 상세/수정페이지 조회/수정저장 공통 응답 DTO",
        example = """
        {
          "studyId": 2,
          "title": "프리토킹 스터디",
          "studyType": "JOB_INTERVIEW",
          "isMine": true,
          "isApplied": false,
          "isApproved": false,
          "teamStatus": "RECRUITING",
          "bannerImageUrl": "https://cdn.example.com/images/default.png",
          "traits": ["정리의 신", "실행력 갓", "내향인"],
          "capacity": 4,
          "applicantCount": 6,
          "mode": "ONLINE",
          "address": null,
          "preferredAges": { "ageMin": 20, "ageMax": 30 },
          "expectedMonth": 3,
          "startDate": "2025-09-10",
          "detail": "저희 정말 멋진 영어공부를 할거예요~ 하고 싶죠?",
          "leader": {
            "userId": 101,
            "nickname": "개구라",
            "profileImageUrl": "https://cdn.example.com/images/user101.png",
            "mainPosition": "풀스택",
            "mainBadge": null,
            "abandonBadge": null,
            "temperature": 36.5,
            "isMine": false,
            "chatRoomId": null,
            "dmRequestPending": true
          },
          "participants": [
            {
              "userId": 201,
              "nickname": "개고루",
              "profileImageUrl": "https://cdn.example.com/images/user201.png",
              "mainPosition": "백엔드",
              "mainBadge": null,
              "abandonBadge": null,
              "temperature": 36.5,
              "isMine": false,
              "chatRoomId": null,
              "dmRequestPending": true
            }
          ],
          "participantsCount": 3
        }
        """
)
public class StudyRecruitmentDetailResponse {

    @Schema(description = "스터디 ID", example = "2")
    private Long studyId;

    @Schema(description = "제목", example = "프리토킹 스터디")
    private String title;

    @Schema(description = "스터디 유형", example = "JOB_INTERVIEW")
    private StudyType studyType;

    @JsonProperty("isMine")
    @Schema(description = "내가 작성자인지 여부", example = "true")
    private boolean IsMine;

    @Schema(description = "팀 상태", example = "RECRUITING")
    private TeamStatus teamStatus;

    @Schema(description = "배너 이미지 URL")
    private String bannerImageUrl;

    @Schema(description = "모집 성향 리스트")
    private List<String> traits;

    @Schema(description = "모집 정원", example = "4")
    private Integer capacity;

    @Schema(description = "지원자 수", example = "6")
    private Integer applicantCount;

    @Schema(description = "진행 방식", example = "ONLINE")
    private StudyMode mode;

    @Schema(description = "주소 (ONLINE이면 null)")
    private String address;

    @Schema(description = "선호 연령대 (무관 시 null)")
    private PreferredAgesDto preferredAges;

    @Schema(description = "예상 진행 개월 수", example = "3")
    private Integer expectedMonth;

    @Schema(description = "시작 예정일", example = "2025-09-10")
    private LocalDate startDate;

    @Schema(description = "상세 설명")
    private String detail;

    @Schema(description = "리더 정보")
    private UserSummaryDto leader;

    @Schema(description = "참여자 리스트(리더 제외)")
    private List<UserSummaryDto> participants;

    @Schema(description = "참여자 수(리더 제외)", example = "3")
    private Integer participantsCount;
}