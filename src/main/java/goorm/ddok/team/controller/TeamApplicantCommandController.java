package goorm.ddok.team.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.team.service.TeamApplicantCommandService;
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
@RequestMapping("/api/teams/{teamId}/applicants")
@RequiredArgsConstructor
public class TeamApplicantCommandController {

    private final TeamApplicantCommandService teamApplicantCommandService;

    @PostMapping("/{applicationId}/approve")
    @Operation(
            summary = "참여 희망자 승인",
            description = "팀 리더가 특정 신청자의 참여 희망을 승인합니다.",
            security = @SecurityRequirement(name = "Authorization"),
            parameters = {
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true,
                            description = "Bearer {accessToken}",
                            examples = @ExampleObject(value = "Bearer eyJhbGciOi...")),
                    @Parameter(name = "teamId", in = ParameterIn.PATH, required = true, description = "팀 ID"),
                    @Parameter(name = "applicationId", in = ParameterIn.PATH, required = true, description = "신청 ID")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 200, "message": "참여 희망을 승인하였습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "400", description = "정원 초과, 이미 처리된 신청 등",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 400, "message": "팀 정원을 초과할 수 없습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "403", description = "리더 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 403, "message": "팀 리더만 승인을 할 수 있습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "404", description = "신청 내역 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "message": "참여 신청 내역을 찾을 수 없습니다.", "data": null }
                                    """)))
    })
    public ResponseEntity<ApiResponseDto<?>> approveApplicant(
            @PathVariable Long teamId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        teamApplicantCommandService.approve(teamId, applicationId, user);
        return ResponseEntity.ok(ApiResponseDto.of(200, "참여 희망을 승인하였습니다.", null));
    }

    @PostMapping("/{applicationId}/reject")
    @Operation(
            summary = "참여 희망자 거절",
            description = "팀 리더가 특정 신청자의 참여 희망을 거절합니다.",
            security = @SecurityRequirement(name = "Authorization"),
            parameters = {
                    @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true,
                            description = "Bearer {accessToken}",
                            examples = @ExampleObject(value = "Bearer eyJhbGciOi...")),
                    @Parameter(name = "teamId", in = ParameterIn.PATH, required = true, description = "팀 ID"),
                    @Parameter(name = "applicationId", in = ParameterIn.PATH, required = true, description = "신청 ID")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거절 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 200, "message": "참여 희망을 거절하였습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "400", description = "이미 처리된 신청 등",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 400, "message": "이미 처리된 신청입니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "403", description = "리더 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 403, "message": "팀 리더만 거절할 수 있습니다.", "data": null }
                                    """))),
            @ApiResponse(responseCode = "404", description = "신청 내역 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "message": "참여 신청 내역을 찾을 수 없습니다.", "data": null }
                                    """)))
    })
    public ResponseEntity<ApiResponseDto<?>> rejectApplicant(
            @PathVariable Long teamId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        teamApplicantCommandService.reject(teamId, applicationId, user);
        return ResponseEntity.ok(ApiResponseDto.of(200, "참여 희망을 거절하였습니다.", null));
    }
}
