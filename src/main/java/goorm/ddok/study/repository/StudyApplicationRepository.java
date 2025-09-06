package goorm.ddok.study.repository;

import goorm.ddok.study.domain.ApplicationStatus;
import goorm.ddok.study.domain.StudyApplication;
import goorm.ddok.study.domain.StudyRecruitment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyApplicationRepository extends JpaRepository<StudyApplication, Long> {

    /** 특정 모집글(StudyRecruitment)에 대한 전체 지원자 수 조회 */
    long countByStudyRecruitment(StudyRecruitment study);

    /** 특정 유저(userId)가 특정 스터디 모집글(studyId)에 지원했는지 여부 확인 */
    Optional<StudyApplication> findByUser_IdAndStudyRecruitment_Id(Long userId, Long studyId);

    /** 특정 모집글(StudyRecruitment)에 대해, 지정한 상태(status)의 지원자 목록 조회 (페이징) */
    Page<StudyApplication> findByStudyRecruitment_IdAndApplicationStatus(Long recruitmentId, ApplicationStatus applicationStatus, Pageable pageable);


}
