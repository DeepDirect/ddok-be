package goorm.ddok.member.repository;

import goorm.ddok.member.domain.TechStack;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    Optional<TechStack> findByName(String techStackName);

    @Query("select t.name from TechStack t where lower(t.name) like lower(concat('%', :keyword, '%'))")
    List<String> findNamesByKeyword(String keyword, Pageable pageable);
}
