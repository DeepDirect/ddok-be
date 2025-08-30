package goorm.ddok.project.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.project.dto.request.ProjectRecruitmentCreateRequest;
import goorm.ddok.project.dto.response.ProjectRecruitmentResponse;
import goorm.ddok.project.service.ProjectRecruitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "ProjectRectuitment", description = "프로젝트 모집 관련 API")
public class ProjectRecruitmentController {

    private final ProjectRecruitmentService projectRecruitmentService;

    @Operation(
            summary = "프로젝트 모집 글 생성",
            description = """
        새로운 프로젝트 모집글을 생성합니다. 요청은 Multipart/Form-Data 형식입니다.
        - request: JSON (아래 예시 참고, Kakao road_address 매핑 LocationDto 허용)
        - bannerImage: 선택 파일
        
        ONLINE 모드 -> location 생략
        OFFLINE 모드 -> location 필수 (region1/2/3, roadName, main/subBuildingNo, zoneNo, latitude(y), longitude(x))
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "프로젝트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(name="성공 예시", value = """
            {
              "status": 201,
              "message": "프로젝트 생성이 성공했습니다.",
              "data": {
                "projectId": 1,
                "userId": 10,
                "nickname": "고라니",
                "leaderPosition": "백엔드",
                "title": "구지라지",
                "teamStatus": "RECRUITING",
                "expectedStart": "2025-09-01",
                "expectedMonth": 3,
                "mode": "ONLINE",
                "location": null,
                "preferredAges": { "ageMin": 20, "ageMax": 30 },
                "capacity": 5,
                "bannerImageUrl": "https://cdn.example.com/images/project-banner.png",
                "traits": ["실행력 갑", "성실함"],
                "positions": ["백엔드", "프론트엔드", "디자이너"],
                "detail": "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?"
              }
            }
            """))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (비로그인 사용자)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 401, "message": "인증이 필요합니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 시작일 과거",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "시작일은 오늘 이후여야 합니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 위치 정보 누락",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "위치 정보가 올바르지 않습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 연령대 범위 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "연령대 범위가 올바르지 않습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 리더 포지션 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "리더 포지션이 모집 포지션에 포함되어야 합니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 - 배너 이미지 업로드 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 500, "message": "배너 이미지 업로드에 실패했습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 - 프로젝트 저장 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 500, "message": "프로젝트 저장 중 오류가 발생했습니다.", "data": null }
                """)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<ProjectRecruitmentResponse>> createProject(
            @RequestPart("request") @Valid ProjectRecruitmentCreateRequest request,
            @RequestPart(value = "bannerImage", required = false) MultipartFile bannerImage,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ProjectRecruitmentResponse response = projectRecruitmentService.createProject(request, bannerImage, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.of(201, "프로젝트 생성이 성공했습니다.", response));
    }

    @Operation(
            summary = "프로젝트 참여 희망 의사 토글",
            description = """
                사용자가 특정 프로젝트에 참여 희망 의사를 표시하거나 취소합니다.
                
                - 처음 신청하는 경우: `appliedPosition` 필드 필수
                - 이미 지원한 경우:
                  - 같은 포지션 → 신청 취소
                  - 다른 포지션 → 에러 발생
                - 리더 본인은 신청 불가
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신청/취소 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "신청 성공", value = """
                                {
                                  "status": 200,
                                  "message": "프로젝트 참여 희망 의사가 신청되었습니다.",
                                  "data": { "isApplied": true }
                                }
                                """),
                                    @ExampleObject(name = "취소 성공", value = """
                                {
                                  "status": 200,
                                  "message": "프로젝트 참여 희망 의사가 취소되었습니다.",
                                  "data": { "isApplied": false }
                                }
                                """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (포지션 누락 등)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "지원 포지션을 선택해야 합니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (비로그인 사용자)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 401, "message": "인증이 필요합니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "403", description = "리더는 참여 불가",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 403, "message": "리더는 참여 신청을 할 수 없습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "404", description = "프로젝트/포지션 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 404, "message": "존재하지 않는 프로젝트입니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "409", description = "이미 다른 포지션에 지원한 경우",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 409, "message": "이미 해당 프로젝트의 다른 포지션에 지원하였습니다.", "data": null }
                """)))
    })
    @PostMapping("/{projectId}/join")
    public ResponseEntity<ApiResponseDto<Map<String, Boolean>>> toggleJoin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String appliedPosition = body != null ? body.get("appliedPosition") : null;
        boolean isApplied = projectRecruitmentService.toggleJoin(userDetails, projectId, appliedPosition);

        String message = isApplied ?
                "프로젝트 참여 희망 의사가 신청되었습니다." :
                "프로젝트 참여 희망 의사가 취소되었습니다.";

        return ResponseEntity.ok(ApiResponseDto.of(200, message, Map.of("isApplied", isApplied)));
    }
}