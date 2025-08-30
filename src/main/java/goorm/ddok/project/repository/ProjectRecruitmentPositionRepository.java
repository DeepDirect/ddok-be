package goorm.ddok.project.repository;

import goorm.ddok.project.domain.ProjectRecruitment;
import goorm.ddok.project.domain.ProjectRecruitmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRecruitmentPositionRepository extends JpaRepository<ProjectRecruitmentPosition, Long> {
    Optional<ProjectRecruitmentPosition> findByProjectRecruitmentIdAndPositionName(Long projectId, String positionName);

    // ProjectRecruitmentPositionRepository.java
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectRecruitmentPosition p " +
            "where p.projectRecruitment = :recruitment " +
            "and p.positionName not in :names")
    int deleteAllByRecruitmentAndNameNotIn(@Param("recruitment") ProjectRecruitment recruitment,
                                           @Param("names") List<String> names);
}
