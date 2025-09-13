package goorm.ddok.member.service;

import goorm.ddok.badge.service.BadgeService;
import goorm.ddok.chat.service.ChatRoomService;
import goorm.ddok.chat.service.DmRequestCommandService;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.member.domain.*;
import goorm.ddok.member.dto.ProfileDto;
import goorm.ddok.member.dto.request.*;
import goorm.ddok.member.repository.*;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerProfileService {

    private final UserRepository userRepository;
    private final UserPositionRepository userPositionRepository;
    private final UserTraitRepository userTraitRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserLocationRepository userLocationRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final TechStackRepository techStackRepository;
    private final UserReputationRepository userReputationRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final BadgeService badgeService;
    private final ChatRoomService chatRoomService;
    private final DmRequestCommandService dmRequestService;

    /* -------- 포지션 수정 -------- */
    public ProfileDto updatePositions(PositionsUpdateRequest req, CustomUserDetails me) {
        User user = requireMe(me);
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        String main = trimToNull(req.getMainPosition());
        if (main == null) throw new GlobalException(ErrorCode.PROFILE_MAIN_POSITION_REQUIRED);

        List<String> subsRaw = (req.getSubPositions() == null) ? List.of()
                : req.getSubPositions();

        // 정리(트림/빈값 제거)
        List<String> subs = subsRaw.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();

        if (subsRaw.size() != 2) {
            throw new GlobalException(ErrorCode.PROFILE_POSITION_TOTAL_MUST_BE_TWO);
        }

        // 메인과 중복 여부
        if (subs.stream().anyMatch(s -> s.equalsIgnoreCase(main))) {
            throw new GlobalException(ErrorCode.PROFILE_POSITION_DUPLICATED);
        }

        // 중복 제거(대소문자 구분 없이 동일 항목 제거)
        subs = subs.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(String::toLowerCase, s -> s, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())
                ));

        // 기존 포지션 일괄 삭제 후 재구성
        var existing = new ArrayList<>(user.getPositions());
        user.getPositions().clear();
        userPositionRepository.deleteAll(existing);

        user.getPositions().add(UserPosition.primaryOf(user, main));
        short ord = 1;
        for (String s : subs) user.getPositions().add(UserPosition.secondaryOf(user, s, ord++));

        userRepository.save(user);
        return buildProfile(user, me);
    }

    /* -------- 트레이트 수정(전체 치환) -------- */
    public ProfileDto updateTraits(TraitsUpdateRequest req, CustomUserDetails me) {
        User user = requireMe(me);
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        var olds = new ArrayList<>(user.getTraits());
        user.getTraits().clear();
        userTraitRepository.deleteAll(olds);

        List<String> traits = (req.getTraits() == null) ? List.of()
                : req.getTraits().stream().map(this::trimToNull).filter(Objects::nonNull).distinct().toList();

        if (traits.isEmpty() || traits.size() > 5) {
            // 새 에러코드 필요
            throw new GlobalException(ErrorCode.TRAIT_COUNT_OUT_OF_RANGE);
        }

        for (String t : traits) user.getTraits().add(UserTrait.builder().user(user).traitName(t).build());

        userRepository.save(user);
        return buildProfile(user, me);
    }

    /* -------- 활동 시간 수정 -------- */
    public ProfileDto updateActiveHours(ActiveHoursRequest req, CustomUserDetails me) {
        User user = requireMe(me);
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        int start = parseHour(req.getStart()); // "09" -> 9
        int end   = parseHour(req.getEnd());   // "18" -> 18

        // 비즈니스 검증: end >= start
        if (end < start) {
            throw new GlobalException(ErrorCode.ACTIVE_HOURS_RANGE_INVALID);
        }

        UserActivity activity = userActivityRepository.findByUserId(user.getId())
                .orElse(UserActivity.builder()
                        .user(user)
                        .activityStartTime(start)
                        .activityEndTime(end)
                        .build());

        activity = activity.toBuilder()
                .activityStartTime(start)
                .activityEndTime(end)
                .build();

        userActivityRepository.save(activity);
        return buildProfile(user, me);
    }



    /* -------- 자기 소개 생성/수정 -------- */
    public ProfileDto upsertContent(ContentUpdateRequest req, CustomUserDetails me) {
        User user = requireMe(me);

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        String newIntroduce = (req.getContent() != null && !req.getContent().isBlank())
                ? req.getContent().trim()
                : null;

        fresh = fresh.toBuilder()
                .introduce(newIntroduce)
                .build();

        userRepository.save(fresh);

        return buildProfile(fresh, me);
    }



    /* -------- 기술 스택 수정(전체 치환) -------- */
    @Transactional
    public ProfileDto updateTechStacks(TechStacksUpdateRequest req, CustomUserDetails me) {
        User meUser = requireMe(me);

        // 1) 입력 정규화 + 대소문자 무시 중복 제거 + 공백 정리
        List<String> names = (req.getTechStacks() == null) ? List.of()
                : req.getTechStacks().stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> s.replaceAll("\\s+", " "))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(String::toLowerCase, s -> s, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())
                ));

        // 2) 기존 UserTechStack 전부 삭제 (조인 테이블 기준)
        userTechStackRepository.deleteByUserId(meUser.getId());

        // 3) 영속 사용자 재조회 (PC 클리어 대비)
        User user = userRepository.findById(meUser.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 4) 필요한 TechStack 엔티티 확보 (없으면 생성)
        //    -> UserTechStack만 추가/재생성 (도메인은 UserTechStack 기준)
        for (String name : names) {
            TechStack stack = techStackRepository.findByName(name)
                    .orElseGet(() -> techStackRepository.save(TechStack.builder().name(name).build()));

            // 이중 추가 방지(이론상 필요 없지만 안전하게)
            boolean exists = user.getTechStacks().stream()
                    .anyMatch(uts -> uts.getTechStack() != null &&
                            Objects.equals(uts.getTechStack().getId(), stack.getId()));
            if (!exists) {
                user.getTechStacks().add(
                        UserTechStack.builder()
                                .user(user)
                                .techStack(stack)
                                .build()
                );
            }
        }

        userRepository.save(user);

        // 5) 최신 프로필로 응답
        return buildProfile(user, me);
    }

    /* -------- 포트폴리오 전체 치환 -------- */
    public ProfileDto upsertPortfolio(PortfolioUpdateRequest req, CustomUserDetails me) { // [CHANGED] 구현
        User user = requireMe(me);
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // null → 빈 목록
        List<PortfolioUpdateRequest.Link> incoming = Optional.ofNullable(req.getPortfolio()).orElse(List.of());

        // (선택) 개수 제한: 20개
        if (incoming.size() > 20) {
            throw new GlobalException(ErrorCode.PORTFOLIO_TOO_MANY); // [NEW] 에러코드 필요
        }

        // 유효성 & 정규화
        List<UserPortfolio> normalized = new ArrayList<>();
        for (PortfolioUpdateRequest.Link link : incoming) {
            if (link == null) continue;

            String title = trimToNull(link.getLinkTitle());
            String url   = trimToNull(link.getLink());

            if (title == null || title.length() > 15) {
                throw new GlobalException(ErrorCode.PORTFOLIO_TITLE_INVALID); // [NEW]
            }
            if (url == null) {
                throw new GlobalException(ErrorCode.PORTFOLIO_URL_REQUIRED);  // [NEW]
            }
            // 아주 간단한 URL 체크 (http/https만 허용)
            if (!url.matches("(?i)^https?://.+")) {
                throw new GlobalException(ErrorCode.PORTFOLIO_URL_INVALID);   // [NEW]
            }

            normalized.add(UserPortfolio.builder()
                    .user(user)
                    .linkTitle(title)
                    .link(url)
                    .build());
        }

        // 기존 전부 삭제 후 전체 치환
        List<UserPortfolio> olds = userPortfolioRepository.findAllByUserId(user.getId());
        if (!olds.isEmpty()) userPortfolioRepository.deleteAllInBatch(olds);
        if (!normalized.isEmpty()) userPortfolioRepository.saveAll(normalized);

        // 변경 후 전체 프로필 반환
        return buildProfile(user, me); // [CHANGED] 프로필 전체 리턴
    }

    /* -------- 공개/비공개 토글 -------- */
    public ProfileDto toggleVisibility(CustomUserDetails me) {
        User meUser = requireMe(me);

        // 최신 상태 로드 (영속 엔티티)
        User fresh = userRepository.findById(meUser.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        boolean current = fresh.isPublic();
        fresh.setPublic(!current);

        userRepository.save(fresh);

        return buildProfile(fresh, me);
    }

    /* -------- 공통: 프로필 빌드 -------- */
    @Transactional(readOnly = true)
    public ProfileDto buildProfile(User user, CustomUserDetails me) {
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Long meId = (me != null && me.getUser() != null) ? me.getUser().getId() : null;

        boolean isMine = meId != null && Objects.equals(meId, fresh.getId());

        Long chatRoomId = null;
        if (!isMine && meId != null) {
            chatRoomId = chatRoomService.findPrivateRoomId(meId, fresh.getId()).orElse(null);
        }

        boolean dmPending = false;
        if (!isMine && meId != null) {
            dmPending = (chatRoomId != null)
                    || dmRequestService.isDmPendingOrAcceptedOrChatExists(meId, fresh.getId());
        }
        BadgeDto mainBadge = badgeService.getRepresentativeGoodBadge(fresh);
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(fresh);

        // temperature (없으면 null)
        BigDecimal temp = userReputationRepository.findByUserId(fresh.getId())
                .map(UserReputation::getTemperature)
                .orElse(null);

        // main/sub positions
        String main = fresh.getPositions().stream()
                .filter(p -> p.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst().orElse(null);

        List<String> subs = fresh.getPositions().stream()
                .filter(p -> p.getType() == UserPositionType.SECONDARY)
                .sorted(Comparator.comparing(UserPosition::getOrd, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(UserPosition::getPositionName)
                .toList();

        // traits
        List<String> traits = fresh.getTraits().stream().map(UserTrait::getTraitName).toList();

        // active hours
        var actOpt = userActivityRepository.findByUserId(fresh.getId());
        ProfileDto.ActiveHours ah = actOpt
                .map(a -> new ProfileDto.ActiveHours(a.getActivityStartTime(), a.getActivityEndTime()))
                .orElse(null);

        // location
        var locOpt = userLocationRepository.findByUserId(fresh.getId());
        ProfileDto.LocationBlock loc = locOpt
                .map(l -> ProfileDto.LocationBlock.builder()
                        .latitude(toDouble(l.getActivityLatitude()))
                        .longitude(toDouble(l.getActivityLongitude()))
                        .address(l.getRoadName())
                        .build())
                .orElse(null);

        List<ProfileDto.PortfolioLink> portfolio =
                userPortfolioRepository.findAllByUserId(fresh.getId()).stream()
                        .map(p -> new ProfileDto.PortfolioLink(p.getLinkTitle(), p.getLink()))
                        .toList();

        // tech stacks
        List<String> stacks = fresh.getTechStacks().stream()
                .map(uts -> uts.getTechStack().getName())
                .toList();

        return ProfileDto.builder()
                .userId(fresh.getId())
                .IsMine(isMine)
                .chatRoomId(chatRoomId)
                .dmRequestPending(dmPending)
                .IsPublic(fresh.isPublic())
                .profileImageUrl(fresh.getProfileImageUrl())
                .nickname(fresh.getNickname())
                .temperature(temp)              // null 허용
                .ageGroup(user.getAgeGroup())
                .mainPosition(main)
                .subPositions(subs)
                .mainBadge(mainBadge)                // 요구사항: 없으면 null
                .abandonBadge(abandonBadge)             // 요구사항: 없으면 null
                .activeHours(ah)
                .traits(traits)
                .content(fresh.getIntroduce())
                .portfolio(portfolio)                // TODO
                .location(loc)
                .techStacks(stacks)
                .build();
    }

    /* -------- helpers -------- */
    private int parseHour(String hh) {
        if (!StringUtils.hasText(hh)) {
            throw new GlobalException(ErrorCode.ACTIVE_HOURS_FORMAT_INVALID);
        }
        try {
            int v = Integer.parseInt(hh);
            if (v < 0 || v > 24) throw new GlobalException(ErrorCode.ACTIVE_HOURS_FORMAT_INVALID);
            return v;
        } catch (NumberFormatException e) {
            throw new GlobalException(ErrorCode.ACTIVE_HOURS_FORMAT_INVALID);
        }
    }

    private User requireMe(CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);
        return me.getUser();
    }
    private String trimToNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private Double toDouble(BigDecimal v) { return (v == null) ? null : v.doubleValue(); }
}