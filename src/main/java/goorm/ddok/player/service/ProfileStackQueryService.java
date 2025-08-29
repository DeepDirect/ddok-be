package goorm.ddok.player.service;

import goorm.ddok.global.exception.ErrorCode;
import goorm.ddok.global.exception.GlobalException;
import goorm.ddok.member.repository.UserRepository;
import goorm.ddok.member.repository.UserTechStackRepository;
import goorm.ddok.player.dto.response.TechStackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileStackQueryService {

    private final UserRepository userRepository;
    private final UserTechStackRepository userTechStackRepository;

    /**
     * 사용자 기술 스택 조회
     */
    public Page<TechStackResponse> getUserTechStacks(Long userId, int page, int size) {
        // 1. 유저 존재 여부 체크
        if (!userRepository.existsById(userId)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 기술 스택 조회
        Pageable pageable = PageRequest.of(page, size);
        return userTechStackRepository.findByUserId(userId, pageable)
                .map(uts -> TechStackResponse.builder()
                        .techStack(uts.getTechStack().getName())
                        .build());
    }
}
