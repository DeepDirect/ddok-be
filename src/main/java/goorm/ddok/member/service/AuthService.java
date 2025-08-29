package goorm.ddok.member.service;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.jwt.JwtTokenProvider;
import goorm.ddok.global.security.token.ReauthTokenService;
import goorm.ddok.global.security.token.RefreshTokenService;
import goorm.ddok.member.domain.*;
import goorm.ddok.member.dto.request.*;
import goorm.ddok.member.dto.response.*;
import goorm.ddok.member.repository.*;
import goorm.ddok.member.util.NicknameGenerator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PhoneVerificationRepository verificationRepository;
    private final UserLocationRepository userLocationRepository;
    private final UserPositionRepository userPositionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserTraitRepository userTraitRepository;
    private final TechStackRepository techStackRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProfileImageService profileImageService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final ReauthTokenService reauthTokenService;

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%]).{8,}$";
    private static final Pattern PASSWORD_REGEX = Pattern.compile(PASSWORD_PATTERN);
    private static final String NAME_PATTERN = "^[가-힣]{2,20}$";
    private static final Pattern NAME_REGEX = Pattern.compile(NAME_PATTERN);


    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        if (!NAME_REGEX.matcher(request.getUsername()).matches()) {
            throw new GlobalException(ErrorCode.INVALID_USERNAME);
        }

        if (!request.getPassword().equals(request.getPasswordCheck())) {
            throw new GlobalException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        if (request.getPassword().length() < 8) {
            throw new GlobalException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        if (!PASSWORD_REGEX.matcher(request.getPassword()).matches()) {
            throw new GlobalException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GlobalException(ErrorCode.DUPLICATE_EMAIL);
        }
        Pattern phonePattern = Pattern.compile("^01[0|1|6|7|8|9][0-9]{7,8}$");
        if (!phonePattern.matcher(request.getPhoneNumber()).matches()) {
            throw new GlobalException(ErrorCode.INVALID_PHONE_FORMAT);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new GlobalException(ErrorCode.PHONE_NUMBER_ALREADY_USED);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        String code = emailVerificationService.createVerification(request.getEmail());
        emailVerificationService.sendVerificationEmail(request.getEmail(), code);

        User user = userRepository.save(User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(encodedPassword)
                .emailVerified(false)
                .build());

        return SignUpResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }

    public boolean isEmailAlreadyExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GlobalException(ErrorCode.DUPLICATE_EMAIL);
        }
        return true;
    }

    @Transactional
    public void setEmailVerificationService(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public SignInResponse signIn(SignInRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new GlobalException(ErrorCode.WRONG_PASSWORD));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new GlobalException(ErrorCode.WRONG_PASSWORD);
        }
// 아직 이메일할 필요가 없으니까 이건 일단 빼놓고
//        if (!user.isEmailVerified()) {
//            emailVerificationService.handleEmailVerification(user.getEmail());
//        }

        String accessToken = jwtTokenProvider.createToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenService.save(user.getId(), refreshToken);

        long ttlMs  = jwtTokenProvider.getRefreshTokenExpireMillis();
        long ttlSec = Math.max(1, ttlMs / 1000);

        String sameSite = "None";
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(ttlSec)
                .sameSite(sameSite)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());


        boolean isPreferences = checkIfUserHasPreferences(user);

        LocationResponse location = null;
        if (user.getLocation() != null) {
            var loc = user.getLocation();
            BigDecimal lat = (loc.getActivityLatitude()  != null) ? loc.getActivityLatitude()  : null;
            BigDecimal lon = (loc.getActivityLongitude() != null) ? loc.getActivityLongitude() : null;

            String address = loc.getRoadName();
            location = new LocationResponse(lat, lon, address);
        }


        SignInUserResponse userDto = new SignInUserResponse(user, isPreferences, location);
        return new SignInResponse(accessToken, userDto);
    }

    private boolean checkIfUserHasPreferences(User user) {
        return user.getLocation() != null &&
                !user.getPositions().isEmpty() &&
                !user.getTraits().isEmpty() &&
                user.getBirthDate() != null &&
                user.getActivity() != null;
    }

    public void signOut(String authorizationHeader, HttpServletResponse response) {
        expireRefreshCookie(response,  "None"); // or "Lax"

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
        String accessToken = authorizationHeader.substring(7);

        jwtTokenProvider.validateToken(accessToken); // 유효하지 않으면 예외(401)

        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        refreshTokenService.delete(userId);
    }

    private void expireRefreshCookie(HttpServletResponse response, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Transactional
    public String findEmail(FindEmailRequest request) {
        AuthType authType = AuthType.FIND_ID;

        PhoneVerification verification = getVerification(authType, request.getPhoneNumber(), request.getPhoneCode());

        if (verification.isVerified()) {
            throw new GlobalException(ErrorCode.ALREADY_VERIFIED);
        }

        User user = userRepository.findByUsernameAndPhoneNumber(
                request.getUsername(), request.getPhoneNumber()
        ).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        verification.verify();
        return user.getEmail();
    }

    public PhoneVerification getVerification(AuthType authType, String phoneNumber, String phoneCode) {
        return verificationRepository
                .findByPhoneNumberAndPhoneCodeAndAuthType(phoneNumber, phoneCode, authType)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    @Transactional
    public String passwordVerifyUser(PasswordVerifyUserRequest request) {
        AuthType authType = AuthType.FIND_PASSWORD;

        PhoneVerification verification = getVerification(authType, request.getPhoneNumber(), request.getPhoneCode());

        if (verification.isVerified()) {
            throw new GlobalException(ErrorCode.ALREADY_VERIFIED);
        }

        userRepository.findByUsernameAndPhoneNumber(
                request.getUsername(), request.getPhoneNumber()
        ).orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        verification.verify();

        String reauthToken = jwtTokenProvider.createReauthenticateToken(
                request.getUsername(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getPhoneCode()
        );

        reauthTokenService.save(request.getEmail(), reauthToken);

        return reauthToken;
    }

    @Transactional
    public void verifyAndResetPassword(PasswordResetRequest request, String authorizationHeader) {
        final AuthType authType = AuthType.FIND_PASSWORD;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }

        String reauthToken = authorizationHeader.replace("Bearer ", "");

        jwtTokenProvider.validateToken(reauthToken); // 예외 방식으로 수정

        Claims claims = jwtTokenProvider.getClaims(reauthToken);
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);
        String phoneNumber = claims.get("phoneNumber", String.class);
        String phoneCode = claims.get("phoneCode", String.class);

        if (!reauthTokenService.isValid(email, reauthToken, username, email, phoneNumber, phoneCode)) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }

        PhoneVerification verification = getVerification(authType, phoneNumber, phoneCode);
        if (!verification.isVerified()) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByEmailAndUsername(email, username)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (!request.getNewPassword().equals(request.getPasswordCheck())) {
            throw new GlobalException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        reauthTokenService.delete(email);
    }

    @Transactional
    public PreferenceResponse createPreference(Long userId, PreferenceRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 1. 포지션 정보 저장
        saveUserPositions(user, request.getMainPosition(), request.getSubPosition());

        // 2. 기술 스택 저장
        saveUserTechStacks(user, request.getTechStacks());

        // 3. 주 활동 지역 저장
        UserLocation userLocation = saveUserLocation(user, request.getLocation());

        // 4. 특성(Traits) 저장
        saveUserTraits(user, request.getTraits());

        // 5. 생년월일 저장
        user.setBirthDate(request.getBirthDate());
        userRepository.save(user);

        // 6. 활동 시간 저장
        saveUserActivity(user, request.getActiveHours());

        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            String nickname = NicknameGenerator.generate(request.getMainPosition());

            int attempts = 0;
            while (userRepository.existsByNickname(nickname) && attempts < 10) {
                nickname = NicknameGenerator.generate(request.getMainPosition());
                attempts++;
            }

            if (userRepository.existsByNickname(nickname)) {
                int suffix = 1;
                String baseNickname = nickname;
                while (userRepository.existsByNickname(nickname)) {
                    nickname = baseNickname + suffix;
                    suffix++;
                }
            }

            user.setNickname(nickname);
        }

        String profileImageUrl = profileImageService.generateProfileImageUrl(user.getNickname(), 48);
        user.setProfileImageUrl(profileImageUrl);

        return buildPreferenceResponse(user, userLocation, request);
    }

    private void saveUserPositions(User user, String mainPosition, List<String> subPositions) {
        UserPosition primaryPosition = UserPosition.primaryOf(user, mainPosition);
        userPositionRepository.save(primaryPosition);

        for (int i = 0; i < subPositions.size(); i++) {
            String subPosition = subPositions.get(i);
            if (subPosition != null && !subPosition.trim().isEmpty()) {
                UserPosition secondaryPosition = UserPosition.secondaryOf(user, subPosition, (short)(i + 1));
                userPositionRepository.save(secondaryPosition);
            }
        }
    }

    private void saveUserTechStacks(User user, List<String> techStacks) {

        if (techStacks != null && !techStacks.isEmpty()) {
            for (String techStackName : techStacks) {
                if (techStackName != null && !techStackName.trim().isEmpty()) {
                    TechStack techStack = techStackRepository.findByName(techStackName)
                            .orElseGet(() -> techStackRepository.save(
                                    TechStack.builder()
                                            .name(techStackName.trim())
                                            .build()
                            ));
                    UserTechStack userTechStack = UserTechStack.builder()
                            .user(user)
                            .techStack(techStack)
                            .build();
                    userTechStackRepository.save(userTechStack);
                }
            }
        }
    }

    private UserLocation saveUserLocation(User user, LocationRequest locationRequest) {
        UserLocation userLocation = userLocationRepository.findByUserId(user.getId())
                .orElse(UserLocation.builder().user(user).build());

        userLocation = userLocation.toBuilder()
                .roadName(locationRequest.getAddress())
                .activityLatitude(locationRequest.getLatitude() == null ? null
                        : new java.math.BigDecimal(String.format(java.util.Locale.US, "%.6f", locationRequest.getLatitude())))
                .activityLongitude(locationRequest.getLongitude() == null ? null
                        : new java.math.BigDecimal(String.format(java.util.Locale.US, "%.6f", locationRequest.getLongitude())))
                .build();

        return userLocationRepository.save(userLocation);
    }

    private void saveUserTraits(User user, List<String> traits) {
        if (traits != null && !traits.isEmpty()) {
            for (String traitName : traits) {
                if (traitName != null && !traitName.trim().isEmpty()) {
                    UserTrait userTrait = UserTrait.builder()
                            .user(user)
                            .traitName(traitName.trim())
                            .build();
                    userTraitRepository.save(userTrait);
                }
            }
        }
    }

    private void saveUserActivity(User user, ActiveHoursRequest activeHoursRequest) {
        Integer startHour = Integer.parseInt(activeHoursRequest.getStart());
        Integer endHour = Integer.parseInt(activeHoursRequest.getEnd());

        UserActivity userActivity = userActivityRepository.findByUserId(user.getId())
                .orElse(UserActivity.builder().user(user).build());

        userActivity = userActivity.toBuilder()
                .activityStartTime(startHour)
                .activityEndTime(endHour)
                .build();

        userActivityRepository.save(userActivity);
    }

    private PreferenceResponse buildPreferenceResponse(User user, UserLocation userLocation, PreferenceRequest request) {
        LocationResponse locationResponse = null;
        if (userLocation != null) {
            BigDecimal lat = (userLocation.getActivityLatitude() != null)
                    ? userLocation.getActivityLatitude() : null;
            BigDecimal lon = (userLocation.getActivityLongitude() != null)
                    ? userLocation.getActivityLongitude() : null;
            locationResponse = new LocationResponse(lat, lon, userLocation.getRoadName());
        }

        ActiveHoursResponse activeHoursResponse = ActiveHoursResponse.builder()
                .start(request.getActiveHours().getStart())
                .end(request.getActiveHours().getEnd())
                .build();

        PreferenceDetailResponse preferences = PreferenceDetailResponse.builder()
                .mainPosition(request.getMainPosition())
                .subPosition(request.getSubPosition())
                .techStacks(request.getTechStacks())
                .location(locationResponse)
                .traits(request.getTraits())
                .birthDate(request.getBirthDate())
                .activeHours(activeHoursResponse)
                .build();


        return PreferenceResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mainPosition(request.getMainPosition())
                .profileImageUrl(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .isPreferences(true)
                .preferences(preferences)
                .build();
    }
}
