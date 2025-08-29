package goorm.ddok.member.repository;

import goorm.ddok.member.domain.User;
import goorm.ddok.member.domain.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    List<UserPortfolio> findByUser(User user);

    List<UserPortfolio> findAllByUserId(Long userId);
}
