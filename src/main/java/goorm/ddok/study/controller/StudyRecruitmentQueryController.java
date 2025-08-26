package goorm.ddok.study.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.service.StudyRecruitmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyRecruitmentQueryController {

    private final StudyRecruitmentQueryService studyRecruitmentQueryService;

    @Operation(
            summary = "스터디 모집글 상세 조회",
            description = """
                특정 스터디 모집글의 상세 정보를 조회합니다.
                
                - 로그인 사용자일 경우 isMine, isApplied, isApproved 값이 반영됩니다.
                - ONLINE 모드일 경우 address = "ONLINE"
                - OFFLINE 모드일 경우 address = "시 구"
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                {
                  "status": 200,
                  "message": "상세 내용 조회가 성공했습니다.",
                  "data": {
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
                    "address": "ONLINE",
                    "preferredAges": { "ageMin": 20, "ageMax": 30 },
                    "expectedMonth": 3,
                    "startDate": "2025-09-10",
                    "detail": "저희 정말 멋진 영어공부를 할거예요~ 하고 싶죠?",
                    "leader": { ... },
                    "participants": [{ ... }],
                    "participantsCount": 3
                  }
                }
                """))),
            @ApiResponse(responseCode = "404", description = "모집글을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 404, "message": "해당 모집글을 찾을 수 없습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "404", description = "리더 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 404, "message": "리더 정보를 찾을 수 없습니다.", "data": null }
                """))),
            @ApiResponse(responseCode = "400", description = "주소 정보가 누락된 경우 (OFFLINE인데 region 정보 없음)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                { "status": 400, "message": "주소 정보가 올바르지 않습니다.", "data": null }
                """)))
    })
    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponseDto<StudyRecruitmentDetailResponse>> getStudyDetail(
            @PathVariable @NotNull Long studyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        StudyRecruitmentDetailResponse response =
                studyRecruitmentQueryService.getStudyDetail(studyId, userDetails);

        return ResponseEntity.ok(ApiResponseDto.of(200, "조회 성공", response));
    }
}