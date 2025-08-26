package goorm.ddok.study.service;

import goorm.ddok.global.dto.PreferredAgesDto;
import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.global.security.auth.CustomUserDetails;
import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPosition;
import goorm.ddok.member.domain.UserPositionType;
import goorm.ddok.study.domain.*;
import goorm.ddok.study.dto.UserSummaryDto;
import goorm.ddok.study.dto.response.StudyRecruitmentDetailResponse;
import goorm.ddok.study.repository.StudyParticipantRepository;
import goorm.ddok.study.repository.StudyRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyRecruitmentQueryService {

    private final StudyRecruitmentRepository studyRecruitmentRepository;
    private final StudyParticipantRepository studyParticipantRepository;

    /**
     * 스터디 모집글 상세 조회
     */
    public StudyRecruitmentDetailResponse getStudyDetail(
            Long studyId,
            CustomUserDetails userDetails
    ) {
        // 스터디 모집글 존재 여부 검증
        StudyRecruitment study = studyRecruitmentRepository.findById(studyId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        User currentUser = (userDetails != null) ? userDetails.getUser() : null;

        // 리더 조회 (없으면 예외)
        StudyParticipant leader = study.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipantRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.LEADER_NOT_FOUND));

        // 참여자 목록 조회
        List<StudyParticipant> participants = studyParticipantRepository.findByStudyRecruitment(study);

        // 응답 DTO 변환
        return StudyRecruitmentDetailResponse.builder()
                .studyId(study.getId())
                .title(study.getTitle())
                .studyType(study.getStudyType())
                .isMine(currentUser != null && study.getUser().getId().equals(currentUser.getId()))
                .isApplied(isApplied(currentUser, participants))
                .isApproved(isApproved(currentUser, participants))
                .teamStatus(study.getTeamStatus())
                .bannerImageUrl(study.getBannerImageUrl())
                .traits(study.getTraits().stream().map(StudyRecruitmentTrait::getTraitName).toList())
                .capacity(study.getCapacity())
                .applicantCount(participants.size())
                .mode(study.getMode())
                .address(resolveAddress(study)) // ONLINE이면 "ONLINE", OFFLINE이면 "시 구"
                .preferredAges(PreferredAgesDto.builder()
                        .ageMin(study.getAgeMin())
                        .ageMax(study.getAgeMax())
                        .build())
                .expectedMonth(study.getExpectedMonths())
                .startDate(study.getStartDate())
                .detail(study.getContentMd())
                .leader(toUserSummaryDto(leader, currentUser))
                .participants(participants.stream()
                        .map(p -> toUserSummaryDto(p, currentUser))
                        .toList())
                .participantsCount(participants.size())
                .build();
    }

    /** 지원 여부 확인 */
    private boolean isApplied(User currentUser, List<StudyParticipant> participants) {
        if (currentUser == null) return false;
        return participants.stream().anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));
    }

    /** 승인 여부 확인 */
    private boolean isApproved(User currentUser, List<StudyParticipant> participants) {
        if (currentUser == null) return false;
        return participants.stream().anyMatch(p ->
                p.getUser().getId().equals(currentUser.getId())
                        && p.getRole() == ParticipantRole.MEMBER
        );
    }

    /** 주소 변환 (ONLINE - "ONLINE", OFFLINE - "시 구") */
    private String resolveAddress(StudyRecruitment study) {
        if (study.getMode() == StudyMode.ONLINE) {
            return "ONLINE";
        }
        if (study.getRegion1DepthName() != null && study.getRegion2DepthName() != null) {
            return study.getRegion1DepthName() + " " + study.getRegion2DepthName();
        }
        throw new GlobalException(ErrorCode.INVALID_LOCATION);
    }


    /** UserSummaryDto 변환 */
    private UserSummaryDto toUserSummaryDto(StudyParticipant participant, User currentUser) {
        User user = participant.getUser();

        // 메인 포지션(PRIMARY)
        String mainPosition = user.getPositions().stream()
                .filter(pos -> pos.getType() == UserPositionType.PRIMARY)
                .map(UserPosition::getPositionName)
                .findFirst()
                .orElse(null);

        // TODO:온도 (온도 테이블 연결 예정)
//        Double temperature = (user.getActivity() != null)
//                ? user.getActivity().getTemperature()
//                : null;

        return UserSummaryDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .mainPosition(mainPosition)
                .temperature(36.5)  // .temperature(temperature)
                .isMine(currentUser != null && user.getId().equals(currentUser.getId()))
                .chatRoomId(null) // TODO: 채팅 연동되면 연결
                .dmRequestPending(false) // TODO: DM 연동되면 연결
                .build();
    }
}