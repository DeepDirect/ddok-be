package goorm.ddok.study.repository;

import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface StudyParticipantRepository extends JpaRepository<StudyParticipant, Long> {

    // 스터디 모집글 기준으로 참가자 전체 조회
    List<StudyParticipant> findByStudyRecruitment(StudyRecruitment study);

}
