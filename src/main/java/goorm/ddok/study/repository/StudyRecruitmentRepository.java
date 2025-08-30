package goorm.ddok.study.repository;

import goorm.ddok.study.domain.StudyRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface StudyRecruitmentRepository extends JpaRepository<StudyRecruitment, Long> {
    Optional<StudyRecruitment> findByIdAndDeletedAtIsNull(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StudyRecruitment s set s.deletedAt = :now where s.id = :id and s.deletedAt is null")
    int softDeleteById(@Param("id") Long id, @Param("now") Instant now);

}
