package goorm.ddok.project.dto.response;

import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.project.domain.ProjectMode;
import goorm.ddok.project.domain.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(
        name = "ProjectRecruitmentResponse",
        description = """
            프로젝트 모집글 상세/생성 응답 DTO.
            - mode 가 ONLINE 이면 location 은 null 일 수 있습니다.
            - location 은 카카오 road_address 매핑 필드로 내려가며,
              프론트는 region + roadName + (main/sub) 를 합쳐 전체 주소를 보여줄 수 있습니다.
            """,
        example = """
        {
          "projectId": 1,
          "userId": 11,
          "nickname": "고라니",
          "leaderPosition": "백엔드",
          "title": "구지라지",
          "teamStatus": "RECRUITING",
          "expectedStart": "2025-09-16",
          "expectedMonth": 3,
          "mode": "OFFLINE",
          "location": {
            "address": "전북 익산시 부송동 망산길 11-17",
            "region1depthName": "전북",
            "region2depthName": "익산시",
            "region3depthName": "부송동",
            "roadName": "망산길",
            "mainBuildingNo": "11",
            "subBuildingNo": "17",
            "zoneNo": "54547",
            "latitude": 35.976749396987046,
            "longitude": 126.99599512792346
          },
          "preferredAges": { "ageMin": 20, "ageMax": 30 },
          "capacity": 6,
          "bannerImageUrl": "https://cdn.example.com/images/default.png",
          "traits": ["정리의 신", "실행력 갓", "내향인"],
          "positions": ["백엔드", "프론트엔드", "디자이너"],
          "detail": "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?"
        }
        """
)
public class ProjectRecruitmentResponse {

    @Schema(description = "프로젝트 ID", example = "1")
    private Long projectId;

    @Schema(description = "리더 사용자 ID", example = "11")
    private Long userId;

    @Schema(description = "리더 닉네임", example = "고라니")
    private String nickname;

    @Schema(description = "리더 포지션", example = "백엔드")
    private String leaderPosition;

    @Schema(description = "공고 제목", example = "구지라지")
    private String title;

    @Schema(description = "팀 상태", example = "RECRUITING")
    private TeamStatus teamStatus;

    @Schema(description = "프로젝트 시작 예정일", example = "2025-09-16")
    private LocalDate expectedStart;

    @Schema(description = "예상 진행 개월 수", example = "3")
    private Integer expectedMonth;

    @Schema(description = "진행 방식 (ONLINE / OFFLINE)", example = "OFFLINE")
    private ProjectMode mode;

    @Schema(
            description = """
                프로젝트 진행 장소 (OFFLINE일 때만 존재).
                Kakao road_address 매핑 필드 사용.
                """,
            implementation = LocationDto.class
    )
    private LocationDto location;

    @Schema(description = "선호 연령대 (무관이면 null)", example = "{\"ageMin\":20,\"ageMax\":30}")
    private PreferredAgesDto preferredAges;

    @Schema(description = "모집 정원", example = "6")
    private Integer capacity;

    @Schema(description = "배너 이미지 URL", example = "https://cdn.example.com/images/default.png")
    private String bannerImageUrl;

    @Schema(description = "모집 성향 리스트", example = "[\"정리의 신\", \"실행력 갓\", \"내향인\"]")
    private List<String> traits;

    @Schema(description = "모집 포지션 리스트", example = "[\"백엔드\", \"프론트엔드\", \"디자이너\"]")
    private List<String> positions;

    @Schema(description = "공고 상세 설명 (Markdown)", example = "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?")
    private String detail;
}