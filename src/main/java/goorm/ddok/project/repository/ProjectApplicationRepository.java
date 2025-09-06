package goorm.ddok.project.repository;

import goorm.ddok.project.domain.ApplicationStatus;
import goorm.ddok.project.domain.ProjectApplication;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import goorm.ddok.project.domain.ProjectRecruitment;
import goorm.ddok.project.domain.ProjectRecruitmentPosition;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    /**
     * 특정 모집글(ProjectRecruitment)에 대한 전체 지원자 수 조회
     */
    int countByPosition_ProjectRecruitment(ProjectRecruitment recruitment);


    /** 프로젝트 기준 전체 지원 수 */
    @Query("""
           select count(a)
           from ProjectApplication a
           where a.position.projectRecruitment.id = :projectId
           """)
    long countAllByProjectId(@Param("projectId") Long projectId);

    /** 내가 특정 포지션에 지원했는지 — 포지션 단위 체크 */
    boolean existsByUser_IdAndPosition_Id(Long userId, Long positionId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectApplication a where a.position.projectRecruitment.id = :projectId")
    void deleteByPosition_ProjectRecruitment_Id(@Param("projectId") Long projectId);
           
    // 내 지원 여부
    boolean existsByUser_IdAndPosition_ProjectRecruitment_Id(Long userId, Long projectId);

    // 특정 포지션 참조 지원 수
    int countByPosition_Id(Long positionId);

    // 프로젝트 참가자(멤버) 제외하고 ‘지원자 현황’ 만들 때 사용할 원본(필요시)
    @Query("""
           select a
           from ProjectApplication a
           where a.position.projectRecruitment.id = :projectId
           """)
    List<ProjectApplication> findAllByProject(Long projectId);

    /**
     * 특정 포지션(ProjectRecruitmentPosition)에 대한 지원자 수 조회
     */
    int countByPosition(ProjectRecruitmentPosition position);

    /**
     * 특정 유저(userId)가 특정 모집글(projectId)에 지원했는지 여부 확인
     * - 결과가 있을 수도 있고 없을 수도 있기 때문에 Optional 로 감싸서 반환
     */
    Optional<ProjectApplication> findByUser_IdAndPosition_ProjectRecruitment_Id(Long userId, Long positionId);

    /**
     * 특정 프로젝트(ProjectRecruitment.id)에 대해, 지정한 상태(status)의 지원자 목록 조회 (페이징)
     */
    Page<ProjectApplication> findByPosition_ProjectRecruitment_IdAndStatus(Long recruitmentId, ApplicationStatus status, Pageable pageable);
}


