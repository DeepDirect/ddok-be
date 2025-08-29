package goorm.ddok.member.repository;

import goorm.ddok.member.domain.UserTechStack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {

    // 다건 + 페이징 조회용
    Page<UserTechStack> findByUserId(Long userId, Pageable pageable);
}
