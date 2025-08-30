package goorm.ddok.study.service;

import goorm.ddok.global.dto.AbandonBadgeDto;
import goorm.ddok.global.dto.BadgeDto;
import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.study.domain.ParticipantRole;
import goorm.ddok.study.domain.StudyMode;
import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import goorm.ddok.study.dto.UserSummaryDto;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.repository.StudyApplicationRepository;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import goorm.ddok.reputation.domain.UserReputation;
import goorm.ddok.reputation.repository.UserReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyRecruitmentQueryService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final StudyParticipantRepository studyParticipantRepository;
    private final StudyApplicationRepository studyApplicationRepository;
    private final UserReputationRepository userReputationRepository;

    /** 스터디 상세 조회 (수정페이지와 동일 스키마) */
    public StudyRecruitmentDetailResponse getStudyDetail(Long studyId, CustomUserDetails userDetails) {
        // 1) 소프트삭제 확인
        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.STUDY_NOT_FOUND));
        if (study.getDeletedAt() != null) throw new GlobalException(ErrorCode.STUDY_NOT_FOUND);

        User me = (userDetails != null) ? userDetails.getUser() : null;

        // 2) 참가자(리더+멤버)
        List<StudyParticipant> all = studyParticipantRepository.findByStudyRecruitment(study);

        // 3) 리더 필수
        StudyParticipant leader = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.LEADER_NOT_FOUND));

        // 4) 멤버/지원자 수
        List<StudyParticipant> members = all.stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .toList();
        int participantsCount = members.size();
        int applicantCount = (int) studyApplicationRepository.countByStudyRecruitment(study);

        // 5) 리더/멤버 요약 (온도/뱃지 없으면 null)
        UserSummaryDto leaderDto = toUserSummaryDto(leader, me);
        List<UserSummaryDto> participantDtos = members.stream()
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getUser().getNickname()).orElse("")))
                .map(p -> toUserSummaryDto(p, me))
                .toList();

        // 6) 주소(ONLINE → null), 선호연령(0/0 → null)
        String address = composeFullAddress(study);
        PreferredAgesDto ages = (isZero(study.getAgeMin()) && isZero(study.getAgeMax()))
                ? null
                : new PreferredAgesDto(study.getAgeMin(), study.getAgeMax());

        // 7) 응답
        return StudyRecruitmentDetailResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .studyType(study.getStudyType())
                .IsMine(me != null && Objects.equals(study.getUser().getId(), me.getId()))
                .teamStatus(study.getTeamStatus())
                .bannerImageUrl(study.getBannerImageUrl())
                .traits(study.getTraits().stream().map(t -> t.getTraitName()).toList())
                .capacity(study.getCapacity())
                .applicantCount(applicantCount)
                .mode(study.getMode())
                .address(address)
                .preferredAges(ages)
                .expectedMonth(study.getExpectedMonths())
                .startDate(study.getStartDate())
                .detail(study.getContentMd())
                .leader(leaderDto)
                .participants(participantDtos)
                .participantsCount(participantsCount)
                .build();
    }

    /* ===== helpers ===== */

    private boolean isZero(Integer v) { return v == null || v == 0; }

    /** 오프라인: r1 r2 r3 road main-sub, 온라인: null */
    private String composeFullAddress(StudyRecruitment s) {
        if (s.getMode() == StudyMode.ONLINE) return null;

        String r1 = Optional.ofNullable(s.getRegion1DepthName()).orElse("");
        String r2 = Optional.ofNullable(s.getRegion2DepthName()).orElse("");
        String r3 = Optional.ofNullable(s.getRegion3DepthName()).orElse("");
        String road = Optional.ofNullable(s.getRoadName()).orElse("");
        String main = Optional.ofNullable(s.getMainBuildingNo()).orElse("");
        String sub  = Optional.ofNullable(s.getSubBuildingNo()).orElse("");

        StringBuilder sb = new StringBuilder();
        if (!r1.isBlank()) sb.append(r1).append(" ");
        if (!r2.isBlank()) sb.append(r2).append(" ");
        if (!r3.isBlank()) sb.append(r3).append(" ");
        if (!road.isBlank()) sb.append(road).append(" ");
        if (!main.isBlank() && !sub.isBlank()) sb.append(main).append("-").append(sub);
        else if (!main.isBlank()) sb.append(main);

        String addr = sb.toString().trim().replaceAll("\\s+", " ");
        return addr.isBlank() ? null : addr;
    }

    /** 요약 변환: 온도/뱃지 “없으면 null” */
    private UserSummaryDto toUserSummaryDto(StudyParticipant participant, User me) {
        User u = participant.getUser();

        String mainPosition = u.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        // 온도: 없으면 null
        BigDecimal temperature = userReputationRepository.findByUserId(u.getId())
                .map(UserReputation::getTemperature)
                .orElse(null);

        // 뱃지: 아직 연동 전 → null
        BadgeDto mainBadge = null;
        AbandonBadgeDto abandonBadge = null;

        return UserSummaryDto.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .mainPosition(mainPosition)
                .mainBadge(mainBadge)          // null
                .abandonBadge(abandonBadge)    // null
                .temperature(temperature)      // null 허용
                .IsMine(me != null && Objects.equals(me.getId(), u.getId()))
                .chatRoomId(null)
                .dmRequestPending(false)
                .build();
    }
}