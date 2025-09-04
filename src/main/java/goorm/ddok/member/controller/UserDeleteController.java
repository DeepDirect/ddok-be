package goorm.ddok.member.controller;

import goorm.ddok.global.response.ApiResponseDto;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserDeleteController {

    private final AuthService accountService;

    @DeleteMapping("/settings")
    @Operation(summary = "회원 탈퇴")
    public ResponseEntity<ApiResponseDto<Void>> deleteMe(@AuthenticationPrincipal CustomUserDetails me) {
        accountService.deleteAccount(me);
        return ResponseEntity.ok(ApiResponseDto.of(200, "회원 탈퇴에 성공했습니다.", null));
    }
}