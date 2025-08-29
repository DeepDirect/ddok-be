package goorm.ddok.player.service;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.repository.UserRepository;
import goorm.ddok.player.dto.response.StudyParticipationResponse;
import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.repository.StudyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileStudyQueryService {

    private final StudyParticipantRepository studyParticipantRepository;
    private final UserRepository userRepository;

    public Page<StudyParticipationResponse> getUserStudies(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<StudyParticipant> participations =
                studyParticipantRepository.findByUserId(userId, pageable);

        return participations.map(StudyParticipationResponse::from);
    }
}
