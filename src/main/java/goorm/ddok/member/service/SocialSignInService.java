package goorm.ddok.member.service;

import goorm.ddok.global.security.jwt.JwtTokenProvider;
import goorm.ddok.global.security.token.RefreshTokenService;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.dto.response.LocationResponse;
import goorm.ddok.member.dto.response.SignInResponse;
import goorm.ddok.member.dto.response.SignInUserResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SocialSignInService {

    private final KakaoOAuthService kakaoOAuthService;
    private final SocialAuthService socialAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignInResponse signInWithKakaoCode(String authorizationCode, String redirectUri, HttpServletResponse response) {

        String kakaoAccessToken = kakaoOAuthService.exchangeCodeForAccessToken(authorizationCode, redirectUri);


        var kuser = kakaoOAuthService.fetchUser(kakaoAccessToken);


        User user = socialAuthService.upsertKakaoUser(
                kuser.kakaoId(), kuser.email(), kuser.nickname(), kuser.profileImageUrl()
        );


        String accessToken  = jwtTokenProvider.createToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());


        refreshTokenService.save(user.getId(), refreshToken);


        long ttlMs  = jwtTokenProvider.getRefreshTokenExpireMillis();
        long ttlSec = Math.max(1, ttlMs / 1000);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(ttlSec)
                .sameSite("None")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());


        LocationResponse location = null;
        if (user.getLocation() != null) {
            var loc = user.getLocation();
            BigDecimal lat = (loc.getActivityLatitude()  != null) ? loc.getActivityLatitude()  : null;
            BigDecimal lon = (loc.getActivityLongitude() != null) ? loc.getActivityLongitude() : null;
            String address = loc.getRoadName();
            location = new LocationResponse(lat, lon, address);
        }

        boolean isPreferences = false;

        SignInUserResponse userDto = new SignInUserResponse(user, isPreferences, location);
        return new SignInResponse(accessToken, userDto);
    }
}