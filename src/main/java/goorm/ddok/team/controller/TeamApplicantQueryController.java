package goorm.ddok.team.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.team.dto.response.TeamApplicantsResponse;
import goorm.ddok.team.service.TeamApplicantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamApplicantQueryController {

    private final TeamApplicantQueryService teamApplicantQueryService;

    @Operation(
            summary = "팀 참여 희망자 조회",
            description = """
                특정 팀(teamId)의 참여 희망자 목록을 조회합니다. 
                리더 및 승인된 팀원만 접근할 수 있으며, 지원 상태가 'PENDING'인 사용자만 조회됩니다.
                """,
            security = @SecurityRequirement(name = "Authorization"),
            parameters = {
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true,
                            description = "Bearer {accessToken}",
                            examples = @ExampleObject(value = "Bearer eyJhbGciOi...")),
                    @Parameter(name = "teamId", in = ParameterIn.PATH, required = true,
                            description = "팀 ID"),
                    @Parameter(name = "page", in = ParameterIn.QUERY, required = false,
                            description = "페이지 번호 (기본값: 0)", example = "0"),
                    @Parameter(name = "size", in = ParameterIn.QUERY, required = false,
                            description = "페이지 크기 (기본값: 4)", example = "4")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "pagination": {
                                          "currentPage": 0,
                                          "pageSize": 4,
                                          "totalPages": 1,
                                          "totalItems": 2
                                        },
                                        "teamId": 2,
                                        "teamType": "PROJECT",
                                        "recruitmentId": 33,
                                        "isLeader": true,
                                        "items": [
                                          {
                                            "applicantId": 101,
                                            "appliedPosition": "프론트엔드",
                                            "status": "PENDING",
                                            "appliedAt": "2025-09-02T12:00:00",
                                            "isMine": false,
                                            "user": {
                                              "userId": 11,
                                              "nickname": "이름최대열두자여서채워봄",
                                              "profileImageUrl": "https://~",
                                              "temperature": 36.5,
                                              "mainPosition": "backend",
                                              "chatRoomId": null,
                                              "dmRequestPending": false,
                                              "mainBadge": {
                                                "type": "login",
                                                "tier": "bronze"
                                              },
                                              "abandonBadge": {
                                                "isGranted": true,
                                                "count": 5
                                              }
                                            }
                                          }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (비로그인 사용자)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 401, "message": "인증이 필요합니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "403", description = "팀원이 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 403, "message": "팀에 소속되지 않은 사용자는 접근할 수 없습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "404", description = "팀 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "message": "팀 정보를 찾을 수 없습니다.", "data": null }
                                    """)))
    })
    @GetMapping("/{teamId}/applicants")
    public ResponseEntity<ApiResponseDto<TeamApplicantsResponse>> getApplicants(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size
    ) {
        TeamApplicantsResponse response = teamApplicantQueryService.getApplicants(teamId, user, page, size);
        return ResponseEntity.ok(ApiResponseDto.of(200, "요청이 성공적으로 처리되었습니다.", response));
    }
}
