package goorm.ddok.member.service;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.domain.SocialAccount;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.repository.SocialAccountRepository;
import goorm.ddok.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PROVIDER_KAKAO = "KAKAO";

    /**
     * 카카오 소셜 로그인 시 사용자 upsert.
     * - 기존 SocialAccount가 있고, 연결 User가 탈퇴 상태(deletedAt != null)이면:
     *   -> 기존 SocialAccount를 삭제하고, 새 User + 새 SocialAccount를 생성
     * - 기존 SocialAccount가 있고, User가 정상 계정이면:
     *   -> 사용자 표시 정보만 업데이트
     * - SocialAccount가 없으면:
     *   -> 새 User + 새 SocialAccount 생성
     */
    @Transactional
    public User upsertKakaoUser(String kakaoId, String email, String kakaoNickname, String profileImageUrl) {
        if (kakaoId == null || kakaoId.isBlank()) {
            throw new IllegalArgumentException("kakaoId is required");
        }

        try {
            return socialAccountRepository.findByProviderAndProviderUserId(PROVIDER_KAKAO, kakaoId)
                    .map(sa -> {
                        User linked = sa.getUser();

                        // 🔥 탈퇴 유저인 경우: 기존 매핑을 제거하고 신규 유저를 생성하여 재연결
                        if (linked.getDeletedAt() != null) {
                            socialAccountRepository.delete(sa);
                            return createAndLinkNewUser(kakaoId, email, kakaoNickname, profileImageUrl);
                        }

                        // 기존 사용자 정보 최신화
                        updateUserFields(linked, kakaoId, email, kakaoNickname, profileImageUrl);
                        return linked;
                    })
                    .orElseGet(() -> createAndLinkNewUser(kakaoId, email, kakaoNickname, profileImageUrl));

        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 Unique 제약 위반 시 재조회
            return socialAccountRepository.findByProviderAndProviderUserId(PROVIDER_KAKAO, kakaoId)
                    .map(SocialAccount::getUser)
                    .orElseThrow(() -> new GlobalException(ErrorCode.SOCIAL_LOGIN_FAILED));
        }
    }

    /* ---------------- internal helpers ---------------- */

    /** 기존 사용자 표시 필드 업데이트(필요 시에만 변경) */
    private void updateUserFields(User u, String kakaoId, String email, String kakaoNickname, String profileImageUrl) {
        // username ← 카카오 닉네임 (사람 표시용)
        String desiredUsername = safeUsernameFromKakaoNickname(kakaoNickname);
        if (!desiredUsername.equals(u.getUsername())) {
            u.setUsername(desiredUsername);
        }

        // 이메일 제공 시 업데이트 (null/blank 무시)
        if (email != null && !email.isBlank() && !email.equals(u.getEmail())) {
            u.setEmail(email);
        }

        // 프로필 이미지 제공 시 업데이트
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            u.setProfileImageUrl(profileImageUrl);
        }
    }

    /** 새 User를 만들고 SocialAccount까지 연결 */
    private User createAndLinkNewUser(String kakaoId, String email, String kakaoNickname, String profileImageUrl) {
        User u = createNewUserFromKakao(kakaoId, email, kakaoNickname, profileImageUrl);

        SocialAccount sa = SocialAccount.builder()
                .provider(PROVIDER_KAKAO)
                .providerUserId(kakaoId)
                .user(u)
                .build();

        socialAccountRepository.save(sa);
        return u;
    }

    /** 카카오 정보로 신규 사용자 생성 (초기 비밀번호 임의 생성/암호화) */
    private User createNewUserFromKakao(String kakaoId, String email, String kakaoNickname, String profileImageUrl) {
        String desiredUsername = safeUsernameFromKakaoNickname(kakaoNickname);
        String safeEmail = (email != null && !email.isBlank()) ? email : null;
        String encodedPw = passwordEncoder.encode(UUID.randomUUID().toString());

        return userRepository.save(
                User.builder()
                        .username(desiredUsername)   // 사람 표시용
                        .nickname(null)              // 별칭은 선호도 설정 등에서 생성 가능
                        .email(safeEmail)
                        .phoneNumber(null)
                        .password(encodedPw)
                        .profileImageUrl(profileImageUrl)
                        .build()
        );
    }

    /** 사람 표시용 username: 카카오 닉네임 없으면 기본값 */
    private String safeUsernameFromKakaoNickname(String kakaoNickname) {
        String base = (kakaoNickname == null || kakaoNickname.isBlank()) ? "카카오사용자" : kakaoNickname.trim();
        return base;
    }

    /** 필요시: 닉네임 12자 제한 대응용 축약기(현재는 미사용) */
    @SuppressWarnings("unused")
    private String compactKakaoNickFromId(String kakaoId) {
        String digits = kakaoId.replaceAll("\\D", "");
        String last10 = (digits.length() >= 10) ? digits.substring(digits.length() - 10) : pad10(digits);
        return "k_" + last10; // "k_" + 10자리 = 12자
    }

    private String pad10(String s) {
        String seed = (s == null ? "" : s) + UUID.randomUUID().toString().replaceAll("\\D", "");
        return seed.substring(0, 10);
    }
}