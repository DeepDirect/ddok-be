package goorm.ddok.project.repository;

import goorm.ddok.project.domain.ParticipantRole;
import goorm.ddok.project.domain.ProjectParticipant;
import goorm.ddok.project.domain.ProjectRecruitment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {
    // 리더/참가자 조회용 기존 메서드들 유지
    Optional<ProjectParticipant> findFirstByPosition_ProjectRecruitment_IdAndRoleAndDeletedAtIsNull(Long projectId, ParticipantRole role);

    List<ProjectParticipant> findByPosition_ProjectRecruitment_IdAndDeletedAtIsNull(Long projectId);

    long countByPosition_IdAndDeletedAtIsNull(Long positionId);

    /** 특정 포지션 + 역할의 참가자 수 (확정자 집계용) */
    long countByPosition_IdAndRoleAndDeletedAtIsNull(Long positionId, ParticipantRole role);

    /** 내가 특정 포지션에 멤버로 확정됐는지 */
    boolean existsByUser_IdAndPosition_IdAndRoleAndDeletedAtIsNull(
            Long userId, Long positionId, ParticipantRole role
    );

    /** 내가 해당 프로젝트(어떤 포지션이든)에서 참가 중인지 (확정 여부/포지션 무관) */
    boolean existsByUser_IdAndPosition_ProjectRecruitment_IdAndDeletedAtIsNull(Long userId, Long projectId);

    // 삭제용
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectParticipant p where p.position.projectRecruitment.id = :projectId")
    void deleteByPosition_ProjectRecruitment_Id(@Param("projectId") Long projectId);

    List<ProjectParticipant> findByPosition_ProjectRecruitment_Id(Long projectId);

    // 특정 포지션을 참조하는 참가자 수
    List<ProjectParticipant> findByPosition_ProjectRecruitment(ProjectRecruitment recruitment);

    /**
     * 특정 사용자(userId)가 참여한 프로젝트 목록 조회 (Soft Delete 제외, 모집중(RECRUITING) 제외)
     */
    Page<ProjectParticipant> findByUser_IdAndDeletedAtIsNullAndPosition_ProjectRecruitment_TeamStatusNot(
            Long userId,
            goorm.ddok.project.domain.TeamStatus status,
            Pageable pageable
    );

    /** 프로젝트 전체 참가자 수 (Soft Delete 제외) */
    long countByPosition_ProjectRecruitment_IdAndDeletedAtIsNull(Long projectId);
}
