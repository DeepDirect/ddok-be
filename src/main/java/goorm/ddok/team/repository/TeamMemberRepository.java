package goorm.ddok.team.repository;

import goorm.ddok.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * 특정 팀(teamId)에 대해, 주어진 사용자(userId)가
     * 현재 승인된 팀원(LEADER or MEMBER)인지 여부 확인
     * - deletedAt 이 null 이어야 함 (탈퇴/추방 안 된 상태)
     */
    boolean existsByTeam_IdAndUser_IdAndDeletedAtIsNull(Long teamId, Long userId);
}
