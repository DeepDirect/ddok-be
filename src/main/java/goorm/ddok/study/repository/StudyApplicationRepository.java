package goorm.ddok.study.repository;

import goorm.ddok.study.domain.ApplicationStatus;
import goorm.ddok.study.domain.StudyApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudyApplicationRepository extends JpaRepository<StudyApplication, Long> {

    Optional<StudyApplication> findByUser_IdAndStudyRecruitment_Id(Long userId, Long studyId);

    long countByStudyRecruitment_Id(Long studyId);

    // 필요 시 상태별 카운트 등 확장 가능
    long countByStudyRecruitment_IdAndApplicationStatus(Long studyId, ApplicationStatus status);

    @Query("""
        select count(a) from StudyApplication a
        where a.studyRecruitment.id = :studyId
    """)
    long countAllByStudyId(Long studyId);
}
