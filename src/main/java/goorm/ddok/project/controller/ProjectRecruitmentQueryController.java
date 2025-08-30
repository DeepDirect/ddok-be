package goorm.ddok.project.controller;


import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.project.dto.response.ProjectDetailResponse;
import goorm.ddok.project.service.ProjectRecruitmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "ProjectRecruitmentQuery", description = "프로젝트 관련 API")
public class ProjectRecruitmentQueryController {

    private final ProjectRecruitmentQueryService projectRecruitmentQueryService;

    /**
     * 프로젝트 모집글 상세 조회
     */
    @Operation(
            summary = "프로젝트 모집 글 상세 조회",
            description = """
            특정 프로젝트 모집글의 상세 정보를 조회합니다.
            - OFFLINE 프로젝트: region1/2/3 + roadName (+건물번호가 저장되어 있으면 함께) 를 합쳐 address 로 반환
            - ONLINE 프로젝트: address 는 null
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상세 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "OFFLINE (주소 합쳐서 반환)",
                                            value = """
                                        {
                                          "status": 200,
                                          "message": "상세 내용 조회가 성공했습니다.",
                                          "data": {
                                            "projectId": 2,
                                            "isMine": true,
                                            "title": "구라라지 프로젝트",
                                            "teamStatus": "RECRUITING",
                                            "bannerImageUrl": "https://cdn.example.com/images/default.png",
                                            "traits": ["정리의 신", "실행력 갓", "내향인"],
                                            "capacity": 4,
                                            "applicantCount": 6,
                                            "mode": "OFFLINE",
                                            "address": "전북 익산시 부송동 망산길 11-17",
                                            "preferredAges": { "ageMin": 20, "ageMax": 30 },
                                            "expectedMonth": 3,
                                            "startDate": "2025-09-10",
                                            "detail": "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?",
                                            "positions": [ { "position": "백엔드", "applied": 1, "confirmed": 0, "isApplied": false, "isApproved": false, "isAvailable": true } ],
                                            "leader": { "userId": 1, "nickname": "고라니", "profileImageUrl": "https://...", "decidedPosition": "PM", "isMine": true },
                                            "participants": [ { "userId": 2, "nickname": "hong", "decidedPosition": "프론트엔드", "isMine": false } ]
                                          }
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "ONLINE (주소 null)",
                                            value = """
                                        {
                                          "status": 200,
                                          "message": "상세 내용 조회가 성공했습니다.",
                                          "data": {
                                            "projectId": 3,
                                            "isMine": false,
                                            "title": "온라인 프로젝트",
                                            "teamStatus": "RECRUITING",
                                            "bannerImageUrl": "https://cdn.example.com/images/default.png",
                                            "traits": [],
                                            "capacity": 5,
                                            "applicantCount": 0,
                                            "mode": "ONLINE",
                                            "address": null,
                                            "preferredAges": null,
                                            "expectedMonth": 2,
                                            "startDate": "2025-10-01",
                                            "detail": "온라인으로 진행해요",
                                            "positions": [],
                                            "leader": { "userId": 11, "nickname": "june", "profileImageUrl": "https://...", "decidedPosition": "백엔드", "isMine": false },
                                            "participants": []
                                          }
                                        }
                                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 프로젝트",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    value = """
                                { "status": 404, "message": "해당 프로젝트 모집글을 찾을 수 없습니다.", "data": null }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "리더 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    value = """
                                { "status": 404, "message": "프로젝트 리더를 찾을 수 없습니다.", "data": null }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 위치 정보",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(
                                    value = """
                                { "status": 400, "message": "위치 정보가 올바르지 않습니다.", "data": null }
                                """
                            )
                    )
            )
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponseDto<ProjectDetailResponse>> getProjectDetail(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProjectDetailResponse response = projectRecruitmentQueryService.getProjectDetail(projectId, userDetails);
        return ResponseEntity.ok(ApiResponseDto.of(200, "상세 내용 조회가 성공했습니다.", response));
    }
}