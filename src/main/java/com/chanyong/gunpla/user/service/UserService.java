package com.chanyong.gunpla.user.service;

import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.user.dto.NicknameUpdateRequest;
import com.chanyong.gunpla.user.dto.UserResponse;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 계정 조회 및 닉네임 수정 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 유저 정보를 조회한다.
     *
     * @param userId 로그인 유저 ID
     * @return 유저 응답 DTO
     * @throws com.chanyong.gunpla.global.exception.BusinessException USER_NOT_FOUND(404)
     */
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 닉네임을 수정한다.
     *
     * @param userId 로그인 유저 ID
     * @param req    변경할 닉네임
     * @return 수정된 유저 응답 DTO
     */
    @Transactional
    public UserResponse updateNickname(Long userId, NicknameUpdateRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(req.nickname());
        return UserResponse.from(user);
    }
}
