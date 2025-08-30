package goorm.ddok.study.controller;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.dto.request.StudyRecruitmentUpdateRequest;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.dto.response.StudyUpdateResultResponse;
import goorm.ddok.study.service.StudyRecruitmentEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyRecruitmentEditController {

    private final StudyRecruitmentEditService Service;

    @GetMapping("/{studyId}/edit")
    public ResponseEntity<ApiResponseDto<StudyRecruitmentDetailResponse>> getEditPage(
            @PathVariable Long studyId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        StudyRecruitmentDetailResponse data = Service.getEditPage(studyId, user);
        return ResponseEntity.ok(ApiResponseDto.of(200, "스터디 수정페이지 조회가 성공했습니다.", data));
    }

    @PatchMapping(
            value = "/{studyId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponseDto<StudyRecruitmentDetailResponse>> updateStudyWithFile(
            @PathVariable Long studyId,
            @RequestPart("request") StudyRecruitmentUpdateRequest request,
            @RequestPart(value = "bannerImage", required = false) MultipartFile bannerImage,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        StudyRecruitmentDetailResponse data =
                Service.updateStudy(studyId, request, bannerImage, userDetails);
        return ResponseEntity.ok(ApiResponseDto.of(200, "스터디 수정이 성공했습니다.", data));
    }

    // (B) 파일 없이 수정 저장: application/json
    @PatchMapping(
            value = "/{studyId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponseDto<StudyRecruitmentDetailResponse>> updateStudyJsonOnly(
            @PathVariable Long studyId,
            @RequestBody StudyRecruitmentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // bannerImage = null
        StudyRecruitmentDetailResponse data =
                Service.updateStudy(studyId, request, null, userDetails);
        return ResponseEntity.ok(ApiResponseDto.of(200, "스터디 수정이 성공했습니다.", data));
    }
}