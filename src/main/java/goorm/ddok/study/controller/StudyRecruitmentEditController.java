package goorm.ddok.study.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.dto.request.StudyRecruitmentUpdateRequest;
import goorm.ddok.study.dto.response.StudyEditPageResponse;
import goorm.ddok.study.dto.response.StudyUpdateResultResponse;
import goorm.ddok.study.service.StudyRecruitmentEditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "StudyRecruitment-Edit", description = "스터디 수정/조회 API")
public class StudyRecruitmentEditController {

    private final StudyRecruitmentEditService service;

    @Operation(
            summary = "스터디 수정 페이지 조회",
            description = "수정 화면 진입 시 필요한 상세 정보/통계를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정하기 페이지 조회가 성공했습니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                {
                  "status": 200,
                  "message": "수정하기 페이지 조회가 성공했습니다.",
                  "data": {
                    "title": "구라라지 스터디",
                    "teamStatus": "RECRUITING",
                    "bannerImageUrl": "https://cdn.example.com/images/default.png",
                    "traits": ["정리의 신","실행력 갓","내향인"],
                    "capacity": 4,
                    "applicantCount": 6,
                    "mode": "online",
                    "address": "online",
                    "preferredAges": { "ageMin": 20, "ageMax": 30 },
                    "expectedMonth": 3,
                    "startDate": "2025-09-10",
                    "detail": "저희 정말 멋진 웹을 만들거에요~ 하고 싶죠?",
                    "studyType": "SELF_DEV"
                  }
                }"""))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "스터디 없음")
    })
    @GetMapping("/{studyId}/edit")
    public ResponseEntity<ApiResponseDto<StudyEditPageResponse>> getEditPage(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        StudyEditPageResponse data = service.getEditPage(studyId, user);
        return ResponseEntity.ok(ApiResponseDto.of(200, "수정하기 페이지 조회가 성공했습니다.", data));
    }

    @Operation(
            summary = "스터디 수정 저장",
            description = "multipart/form-data 요청. request 파트는 JSON, bannerImage는 선택 파일."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청이 성공적으로 처리되었습니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponseDto.class),
                            examples = @ExampleObject(value = """
                {
                  "status": 200,
                  "message": "요청이 성공적으로 처리되었습니다.",
                  "data": {
                    "studyId": 1,
                    "title": "구지라지",
                    "teamStatus": "RECRUITING",
                    "bannerImageUrl": "https://cdn.example.com/images/default.png",
                    "traits": ["정리의 신","실행력 갓","내향인"],
                    "capacity": 6,
                    "applicantCount": 6,
                    "mode": "offline",
                    "address": "서울특별시 강남구 테헤란로…",
                    "preferredAges": { "ageMin": 20, "ageMax": 30 },
                    "expectedMonth": 3,
                    "startDate": "2025-08-16",
                    "detail": "저희 정말 멋진 영어공부를 할거예요~ 하고 싶죠?",
                    "studyType": "SELF_DEV"
                  }
                }"""))),
            @ApiResponse(responseCode = "400", description = "검증 실패/규칙 위반"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "스터디 없음"),
            @ApiResponse(responseCode = "500", description = "배너 업로드 실패 등 서버 오류")
    })
    @PatchMapping(value = "/{studyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<StudyUpdateResultResponse>> update(
            @PathVariable Long studyId,
            @RequestPart("request") @Valid StudyRecruitmentUpdateRequest request,
            @RequestPart(value = "bannerImage", required = false) MultipartFile bannerImage,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        StudyUpdateResultResponse data = service.updateStudy(studyId, request, bannerImage, user);
        return ResponseEntity.ok(ApiResponseDto.of(200, "요청이 성공적으로 처리되었습니다.", data));
    }
}