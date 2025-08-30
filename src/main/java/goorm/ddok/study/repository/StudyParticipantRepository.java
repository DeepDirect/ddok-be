package goorm.ddok.study.repository;

import goorm.ddok.study.domain.StudyParticipant;
import goorm.ddok.study.domain.StudyRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;


public interface StudyParticipantRepository extends JpaRepository<StudyParticipant, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update StudyParticipant p
              set p.deletedAt = :now
            where p.studyRecruitment.id = :studyId
              and p.deletedAt is null
           """)
    int softDeleteByStudyId(@Param("studyId") Long studyId, @Param("now") Instant now);

    // 스터디 모집글 기준으로 참가자 전체 조회
    List<StudyParticipant> findByStudyRecruitment(StudyRecruitment study);

    List<StudyParticipant> findByStudyRecruitment_IdAndDeletedAtIsNull(Long studyId);

}
