package goorm.ddok.project.service;

import goorm.ddok.badge.service.BadgeService;
import goorm.ddok.chat.service.ChatRoomService;
import goorm.ddok.chat.service.DmRequestCommandService;
import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.LocationDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.file.FileService;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.global.util.BannerImageService;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import goorm.ddok.project.domain.*;
import goorm.ddok.project.dto.ProjectPositionDto;
import goorm.ddok.project.dto.ProjectUserSummaryDto;
import goorm.ddok.project.dto.request.ProjectRecruitmentUpdateRequest;
import goorm.ddok.project.dto.response.ProjectDetailResponse;
import goorm.ddok.project.dto.response.ProjectUpdateResultResponse;
import goorm.ddok.project.repository.ProjectApplicationRepository;
import goorm.ddok.project.repository.ProjectParticipantRepository;
import goorm.ddok.project.repository.ProjectRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectRecruitmentEditService {

    private final ProjectRecruitmentRepository recruitmentRepository;
    private final ProjectParticipantRepository participantRepository;
    private final ProjectApplicationRepository applicationRepository;
    private final UserReputationRepository userReputationRepository;
    private final FileService fileService;
    private final BannerImageService bannerImageService;
    private final BadgeService badgeService;
    private final ChatRoomService chatRoomService;
    private final DmRequestCommandService dmRequestService;


    /* =========================
     *  수정 페이지 조회 (상세와 동일 스키마)
     * ========================= */
    @Transactional(readOnly = true)
    public ProjectDetailResponse getEditPage(Long projectId, CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        ProjectRecruitment pr = recruitmentRepository.findById(projectId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PROJECT_NOT_FOUND));
        ensureNotDeleted(pr);

        // 리더만 조회 가능
        if (!Objects.equals(pr.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        // 참가자(리더 포함)
        List<ProjectParticipant> participants =
                participantRepository.findByPosition_ProjectRecruitment_IdAndDeletedAtIsNull(projectId);

        ProjectParticipant leader = participants.stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.LEADER_NOT_FOUND));

        // 총 지원자 수
        long applicantCount = applicationRepository.countAllByProjectId(projectId);

        Long meId = me.getUser().getId();

        // 포지션별 현황 (확정자=Participant MEMBER 수, isApplied/isApproved=포지션별)
        List<ProjectPositionDto> positionDtos = pr.getPositions().stream()
                .map(position -> {
                    long confirmedCount = participantRepository
                            .countByPosition_IdAndRoleAndDeletedAtIsNull(position.getId(), ParticipantRole.MEMBER);

                    int appliedForPos = applicationRepository.countByPosition_Id(position.getId());

                    boolean isApplied = applicationRepository
                            .existsByUser_IdAndPosition_Id(meId, position.getId());

                    boolean isApproved = participantRepository
                            .existsByUser_IdAndPosition_IdAndRoleAndDeletedAtIsNull(
                                    meId, position.getId(), ParticipantRole.MEMBER);

                    boolean alreadyAppliedOtherPos = applicationRepository
                            .existsByUser_IdAndPosition_ProjectRecruitment_Id(meId, pr.getId()) && !isApplied;

                    boolean alreadyMemberAnyPos = participantRepository
                            .existsByUser_IdAndPosition_ProjectRecruitment_IdAndDeletedAtIsNull(meId, pr.getId());

                    boolean isAvailable = (pr.getTeamStatus() == TeamStatus.RECRUITING)
                            && (confirmedCount < pr.getCapacity())
                            && !alreadyMemberAnyPos
                            && !alreadyAppliedOtherPos;

                    return ProjectPositionDto.builder()
                            .position(position.getPositionName())
                            .applied(appliedForPos)
                            .confirmed((int) confirmedCount)
                            .IsApplied(isApplied)
                            .IsApproved(isApproved)
                            .IsAvailable(isAvailable)
                            .build();
                })
                .toList();

        LocationDto location = buildLocationForRead(pr);

        ProjectUserSummaryDto leaderDto = toUserSummaryDto(leader, me.getUser());
        List<ProjectUserSummaryDto> memberDtos = participants.stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .map(p -> toUserSummaryDto(p, me.getUser()))
                .toList();

        PreferredAgesDto ages = PreferredAgesDto.of(
                (pr.getAgeMin() == 0 ? null : pr.getAgeMin()),
                (pr.getAgeMax() == 0 ? null : pr.getAgeMax())
        );

        return ProjectDetailResponse.builder()
                .projectId(pr.getId())
                .IsMine(true)
                .title(pr.getTitle())
                .teamStatus(pr.getTeamStatus())
                .bannerImageUrl(pr.getBannerImageUrl())
                .traits(pr.getTraits().stream().map(ProjectRecruitmentTrait::getTraitName).toList())
                .capacity(pr.getCapacity())
                .applicantCount((int) applicantCount)
                .mode(pr.getProjectMode())
                .location(location)
                .preferredAges(ages)
                .expectedMonth(pr.getExpectedMonths())
                .startDate(pr.getStartDate())
                .detail(pr.getContentMd())
                .positions(positionDtos)
                .leader(leaderDto)
                .participants(memberDtos)
                .build();
    }

    /* =========================
     *  수정 저장
     * ========================= */
    public ProjectUpdateResultResponse updateProject(Long projectId,
                                                     ProjectRecruitmentUpdateRequest req,
                                                     MultipartFile bannerImage,
                                                     CustomUserDetails me) {
        if (me == null || me.getUser() == null) throw new GlobalException(ErrorCode.UNAUTHORIZED);

        ProjectRecruitment pr = recruitmentRepository.findById(projectId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PROJECT_NOT_FOUND));
        ensureNotDeleted(pr);

        // 리더만 수정
        if (!Objects.equals(pr.getUser().getId(), me.getUser().getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        // 과거 시작일 금지
        if (req.getExpectedStart() != null && req.getExpectedStart().isBefore(LocalDate.now())) {
            throw new GlobalException(ErrorCode.INVALID_START_DATE);
        }

        // 위치 검증
        if (req.getMode() == ProjectMode.offline) {
            LocationDto loc = req.getLocation();
            if (loc == null || loc.getLatitude() == null || loc.getLongitude() == null) {
                throw new GlobalException(ErrorCode.INVALID_LOCATION);
            }
        }

        // 포지션 정규화
        if (req.getPositions() == null || req.getPositions().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_POSITIONS);
        }
        List<String> desiredPositions = req.getPositions().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        // 리더 포지션 포함 확인
        if (!desiredPositions.contains(req.getLeaderPosition())) {
            throw new GlobalException(ErrorCode.INVALID_LEADER_POSITION);
        }

        // 정원 vs 포지션 개수 검증 (요청에 capacity 없으면 기존 값으로 비교)
        int newCapacity = Optional.ofNullable(req.getCapacity()).orElse(pr.getCapacity());
        if (desiredPositions.size() > newCapacity) {
            throw new GlobalException(ErrorCode.INVALID_CAPACITY_POSITIONS);
        }

        // 연령 무관(null) 또는 10단위 강제
        int ageMin;
        int ageMax;
        if (req.getPreferredAges() == null) {
            ageMin = 0;
            ageMax = 0;
        } else {
            ageMin = req.getPreferredAges().getAgeMin();
            ageMax = req.getPreferredAges().getAgeMax();
            if (ageMin > ageMax) throw new GlobalException(ErrorCode.INVALID_AGE_RANGE);
            if (ageMin % 10 != 0 || ageMax % 10 != 0) {
                throw new GlobalException(ErrorCode.INVALID_AGE_BUCKET);
            }
        }

        String bannerUrl;
        // 배너: 파일 > 요청 URL > 기존 > 기본
        if (req.getBannerImageUrl() != null){
            bannerUrl = req.getBannerImageUrl();
        } else if (bannerImage != null && !bannerImage.isEmpty()){
            bannerUrl = uploadBannerImage(bannerImage);
        } else {
            bannerUrl = bannerImageService.generateBannerImageUrl(req.getTitle(), "PROJECT", 1200, 600);
        }
//        String bannerUrl= (bannerImage != null && !bannerImage.isEmpty())
//                ? uploadBannerImage(bannerImage)
//                : bannerImageService.generateBannerImageUrl(req.getTitle(), "PROJECT", 1200, 600);

        // 위치 업데이트 (카카오 필드 개별 저장)
        boolean offline = req.getMode() == ProjectMode.offline;
        if (offline) {
            pr = applyOfflineLocation(pr, req.getLocation());
        } else {
            pr = clearLocation(pr);
        }

        // 기본 필드 업데이트
        pr = pr.toBuilder()
                .title(req.getTitle())
                .teamStatus(req.getTeamStatus())
                .startDate(req.getExpectedStart())
                .expectedMonths(req.getExpectedMonth())
                .projectMode(req.getMode())
                .bannerImageUrl(bannerUrl)
                .contentMd(req.getDetail())
                .capacity(newCapacity)
                .ageMin(ageMin)
                .ageMax(ageMax)
                .build();

        // Traits 머지
        mergeTraits(pr, req.getTraits());

        // Positions 머지(요청에 없는 것은 참조 있으면 에러)
        mergePositionsStrict(pr, desiredPositions);

        // 리더 포지션 동기화
        syncLeaderPositionTo(pr, req.getLeaderPosition());

        ProjectRecruitment saved = recruitmentRepository.save(pr);

        return buildUpdateResult(saved, me);
    }

    /* ---------- soft delete 공통 체크 ---------- */
    private void ensureNotDeleted(ProjectRecruitment pr) {
        if (pr.getDeletedAt() != null) {
            throw new GlobalException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    /* ---------- merge helpers ---------- */

    private void mergeTraits(ProjectRecruitment pr, List<String> incoming) {
        List<String> desired = (incoming == null) ? List.of() : incoming.stream()
                .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();

        Map<String, ProjectRecruitmentTrait> current = pr.getTraits().stream()
                .collect(Collectors.toMap(ProjectRecruitmentTrait::getTraitName, t -> t, (a, b) -> a));

        for (String name : desired) {
            if (!current.containsKey(name)) {
                pr.getTraits().add(ProjectRecruitmentTrait.builder()
                        .projectRecruitment(pr)
                        .traitName(name)
                        .build());
            }
        }
        pr.getTraits().removeIf(t -> !desired.contains(t.getTraitName()));
    }

    private String uploadBannerImage(MultipartFile file) {
        try {
            return fileService.upload(file);
        } catch (IOException e) {
            throw new GlobalException(ErrorCode.BANNER_UPLOAD_FAILED);
        }
    }

    private void mergePositionsStrict(ProjectRecruitment pr, List<String> desired) {
        Map<String, ProjectRecruitmentPosition> byName = pr.getPositions().stream()
                .collect(Collectors.toMap(ProjectRecruitmentPosition::getPositionName, p -> p, (a, b) -> a));

        // 1) 필요한 포지션 추가
        for (String name : desired) {
            if (!byName.containsKey(name)) {
                pr.getPositions().add(ProjectRecruitmentPosition.builder()
                        .projectRecruitment(pr)
                        .positionName(name)
                        .build());
            }
        }

        // 2) 삭제 후보 산정
        List<ProjectRecruitmentPosition> toRemove = pr.getPositions().stream()
                .filter(pos -> !desired.contains(pos.getPositionName()))
                .toList();

        // 3) 참조 여부 확인 → 있으면 에러
        for (ProjectRecruitmentPosition pos : toRemove) {
            long refByParticipants = participantRepository.countByPosition_IdAndDeletedAtIsNull(pos.getId());
            long refByApplications = applicationRepository.countByPosition_Id(pos.getId());
            if (refByParticipants > 0 || refByApplications > 0) {
                throw new GlobalException(ErrorCode.POSITION_IN_USE);
            }
        }

        // 4) 실제 삭제 (참조 없음을 보장한 상태)
        pr.getPositions().removeIf(pos -> !desired.contains(pos.getPositionName()));
    }

    /** 리더 참가자의 포지션을 새 이름으로 동기화 */
    private void syncLeaderPositionTo(ProjectRecruitment pr, String leaderPositionName) {
        if (leaderPositionName == null) return;

        ProjectRecruitmentPosition target = pr.getPositions().stream()
                .filter(p -> p.getPositionName().equals(leaderPositionName))
                .findFirst()
                .orElse(null);
        if (target == null) return;

        participantRepository
                .findFirstByPosition_ProjectRecruitment_IdAndRoleAndDeletedAtIsNull(pr.getId(), ParticipantRole.LEADER)
                .ifPresent(leader -> leader.changePosition(target));
    }

    /* ---------- 주소/위치 helpers ---------- */

    /** 카카오 road_address 필드를 엔티티 각 컬럼에 저장 */
    private ProjectRecruitment applyOfflineLocation(ProjectRecruitment pr, LocationDto loc) {
        return pr.toBuilder()
                .region1depthName(loc.getRegion1depthName())
                .region2depthName(loc.getRegion2depthName())
                .region3depthName(loc.getRegion3depthName())
                .roadName(loc.getRoadName())
                .mainBuildingNo(loc.getMainBuildingNo())
                .subBuildingNo(loc.getSubBuildingNo())
                .zoneNo(loc.getZoneNo())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .build();
    }

    /** online 등 위치 초기화 */
    private ProjectRecruitment clearLocation(ProjectRecruitment pr) {
        return pr.toBuilder()
                .region1depthName(null)
                .region2depthName(null)
                .region3depthName(null)
                .roadName(null)
                .mainBuildingNo(null)
                .subBuildingNo(null)
                .zoneNo(null)
                .latitude(null)
                .longitude(null)
                .build();
    }

    /* ---------- 배너 helper ---------- */
    private String resolveBannerUrl(MultipartFile file, String requestUrl, String currentUrl, String titleForDefault) {
        if (file != null && !file.isEmpty()) {
            try {
                return fileService.upload(file);
            } catch (IOException e) {
                throw new GlobalException(ErrorCode.BANNER_UPLOAD_FAILED);
            }
        }
        if (requestUrl != null && !requestUrl.isBlank()) return requestUrl.trim();
        if (currentUrl != null && !currentUrl.isBlank()) return currentUrl;
        return bannerImageService.generateBannerImageUrl(
                (titleForDefault == null ? "PROJECT" : titleForDefault), "PROJECT", 1200, 600
        );
    }

    /* ---------- 업데이트 응답 빌더 (leader/participants 채움) ---------- */

    private ProjectUpdateResultResponse buildUpdateResult(ProjectRecruitment pr, CustomUserDetails me) {
        Long projectId = pr.getId();

        long applicantCount = applicationRepository.countAllByProjectId(projectId);

        Long meId = (me != null && me.getUser() != null) ? me.getUser().getId() : null;

        // 포지션별 집계/상태를 직접 계산
        List<ProjectUpdateResultResponse.PositionItem> positionItems = pr.getPositions().stream()
                .map(p -> {
                    long confirmedCount = participantRepository
                            .countByPosition_IdAndRoleAndDeletedAtIsNull(p.getId(), ParticipantRole.MEMBER);

                    long appliedCount = applicationRepository.countByPosition_Id(p.getId());

                    boolean isApplied = (meId != null) && applicationRepository
                            .existsByUser_IdAndPosition_Id(meId, p.getId());

                    boolean isApproved = (meId != null) && participantRepository
                            .existsByUser_IdAndPosition_IdAndRoleAndDeletedAtIsNull(meId, p.getId(), ParticipantRole.MEMBER);

                    boolean alreadyAppliedOtherPos = (meId != null)
                            && applicationRepository.existsByUser_IdAndPosition_ProjectRecruitment_Id(meId, pr.getId())
                            && !isApplied;

                    boolean alreadyMemberAnyPos = (meId != null)
                            && participantRepository.existsByUser_IdAndPosition_ProjectRecruitment_IdAndDeletedAtIsNull(meId, pr.getId());

                    boolean isAvailable = (pr.getTeamStatus() == TeamStatus.RECRUITING)
                            && (confirmedCount < pr.getCapacity())
                            && !alreadyMemberAnyPos
                            && !alreadyAppliedOtherPos;

                    return ProjectUpdateResultResponse.PositionItem.builder()
                            .position(p.getPositionName())
                            .applied(appliedCount)
                            .confirmed(confirmedCount)
                            .IsApplied(isApplied)
                            .IsApproved(isApproved)
                            .IsAvailable(isAvailable)
                            .build();
                })
                .toList();

        LocationDto location = buildLocationForRead(pr);
        boolean isMine = meId != null && Objects.equals(pr.getUser().getId(), meId);

        // 무관(0/0)일 때 null
        PreferredAgesDto prefAges = PreferredAgesDto.of(
                (pr.getAgeMin() == 0 ? null : pr.getAgeMin()),
                (pr.getAgeMax() == 0 ? null : pr.getAgeMax())
        );

        // 리더/참여자 조회
        List<ProjectParticipant> participants =
                participantRepository.findByPosition_ProjectRecruitment_IdAndDeletedAtIsNull(projectId);

        ProjectParticipant leader = participants.stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElse(null);

        ProjectUpdateResultResponse.LeaderBlock leaderBlock = buildLeaderBlock(leader, isMine);
        List<ProjectUpdateResultResponse.ParticipantBlock> participantBlocks = buildParticipantBlocks(participants, meId);

        return ProjectUpdateResultResponse.builder()
                .projectId(projectId)
                .IsMine(isMine)
                .title(pr.getTitle())
                .teamStatus(pr.getTeamStatus().name())
                .bannerImageUrl(pr.getBannerImageUrl())
                .traits(pr.getTraits().stream().map(ProjectRecruitmentTrait::getTraitName).toList())
                .capacity(pr.getCapacity())
                .applicantCount(applicantCount)
                .mode(pr.getProjectMode().name().toLowerCase())
                .location(location)
                .preferredAges(prefAges)
                .expectedMonth(pr.getExpectedMonths())
                .startDate(pr.getStartDate())
                .detail(pr.getContentMd())
                .positions(positionItems)
                .leader(leaderBlock)
                .participants(participantBlocks)
                .build();
    }

    /** LeaderBlock 구성: 온도/배지 조회 실패 시 null 폴백 */
    private ProjectUpdateResultResponse.LeaderBlock buildLeaderBlock(ProjectParticipant leader, boolean isMine) {
        if (leader == null) return null;

        User u = leader.getUser();

        String mainPosition = u.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        Double temperature = userReputationRepository.findByUserId(u.getId())
                .map(UserReputation::getTemperature)
                .map(BigDecimal::doubleValue)
                .orElse(null);

        BadgeDto mainBadge = badgeService.getRepresentativeGoodBadge(u);
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(u);

        return ProjectUpdateResultResponse.LeaderBlock.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .mainPosition(mainPosition)
                .mainBadge(mainBadge)
                .abandonBadge(abandonBadge)
                .temperature(temperature)
                .decidedPosition(leader.getPosition() != null ? leader.getPosition().getPositionName() : null)
                .IsMine(isMine)
                .chatRoomId(null)
                .dmRequestPending(false)
                .build();
    }

    /** ParticipantBlock: 온도/배지 조회 실패 시 null 폴백 */
    private List<ProjectUpdateResultResponse.ParticipantBlock> buildParticipantBlocks(List<ProjectParticipant> participants,
                                                                                      Long meId) {
        if (participants == null) return List.of();

        return participants.stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .map(p -> {
                    User u = p.getUser();

                    String mainPosition = u.getPositions().stream()
                            .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                            .map(UserPosition::getPositionName)
                            .findFirst()
                            .orElse(null);

                    Double temperature = userReputationRepository.findByUserId(u.getId())
                            .map(UserReputation::getTemperature)
                            .map(BigDecimal::doubleValue)
                            .orElse(null);

                    BadgeDto mainBadge = badgeService.getRepresentativeGoodBadge(u);
                    AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(u);

                    Long chatRoomId = null;
                    boolean dmPending = false;
                    if (meId != null && !Objects.equals(meId, u.getId())) {
                        chatRoomId = chatRoomService.findPrivateRoomId(meId, u.getId()).orElse(null);
                        dmPending = (chatRoomId != null)
                                || dmRequestService.isDmPendingOrAcceptedOrChatExists(meId, u.getId()); // ✅
                    }


                    return ProjectUpdateResultResponse.ParticipantBlock.builder()
                            .userId(u.getId())
                            .nickname(u.getNickname())
                            .profileImageUrl(u.getProfileImageUrl())
                            .mainPosition(mainPosition)
                            .mainBadge(mainBadge)
                            .abandonBadge(abandonBadge)
                            .temperature(temperature)
                            .decidedPosition(p.getPosition() != null ? p.getPosition().getPositionName() : null)
                            .IsMine(meId != null && Objects.equals(meId, u.getId()))
                            .chatRoomId(chatRoomId)
                            .dmRequestPending(dmPending)
                            .build();
                })
                .toList();
    }

    /* ---------- 편집 페이지용 요약 DTO (온도/배지 null 폴백) ---------- */
    private ProjectUserSummaryDto toUserSummaryDto(ProjectParticipant participant, User currentUser) {
        User u = participant.getUser();

        String mainPosition = u.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        BigDecimal temperature = userReputationRepository.findByUserId(u.getId())
                .map(UserReputation::getTemperature)
                .orElse(null);

        BadgeDto mainBadge = badgeService.getRepresentativeGoodBadge(u);
        AbandonBadgeDto abandonBadge = badgeService.getAbandonBadge(u);

        Long chatRoomId = null;
        boolean dmPending = false;
        if (currentUser != null && !Objects.equals(currentUser.getId(), u.getId())) {
            chatRoomId = chatRoomService.findPrivateRoomId(currentUser.getId(), u.getId()).orElse(null);
            dmPending = (chatRoomId != null)
                    || dmRequestService.isDmPendingOrAcceptedOrChatExists(currentUser.getId(), u.getId()); // ✅
        }

        return ProjectUserSummaryDto.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .mainPosition(mainPosition)
                .mainBadge(mainBadge)
                .abandonBadge(abandonBadge)
                .temperature(temperature)
                .decidedPosition(participant.getPosition() != null ? participant.getPosition().getPositionName() : null)
                .IsMine(currentUser != null && Objects.equals(currentUser.getId(), u.getId()))
                .chatRoomId(chatRoomId)
                .dmRequestPending(dmPending)
                .build();
    }

    private LocationDto buildLocationForRead(ProjectRecruitment pr) {
        if (pr.getProjectMode() == ProjectMode.online) return null;
        String full = composeFullAddress(
                pr.getRegion1depthName(), pr.getRegion2depthName(), pr.getRegion3depthName(),
                pr.getRoadName(), pr.getMainBuildingNo(), pr.getSubBuildingNo()
        );
        return LocationDto.builder()
                .address(full)
                .region1depthName(pr.getRegion1depthName())
                .region2depthName(pr.getRegion2depthName())
                .region3depthName(pr.getRegion3depthName())
                .roadName(pr.getRoadName())
                .mainBuildingNo(pr.getMainBuildingNo())
                .subBuildingNo(pr.getSubBuildingNo())
                .zoneNo(pr.getZoneNo())
                .latitude(pr.getLatitude())
                .longitude(pr.getLongitude())
                .build();
    }

    // 재사용용 유틸 (기존 composeAddress 로직의 문자열 합성 부분만 추출)
    private String composeFullAddress(String r1, String r2, String r3, String road, String main, String sub) {
        StringBuilder sb = new StringBuilder();
        if (r1 != null && !r1.isBlank()) sb.append(r1).append(" ");
        if (r2 != null && !r2.isBlank()) sb.append(r2).append(" ");
        if (r3 != null && !r3.isBlank()) sb.append(r3).append(" ");
        if (road != null && !road.isBlank()) sb.append(road).append(" ");
        if (main != null && !main.isBlank()) {
            sb.append(main);
            if (sub != null && !sub.isBlank()) sb.append("-").append(sub);
        }
        String s = sb.toString().trim().replaceAll("\\s+", " ");
        return s.isBlank() ? null : s;
    }

}