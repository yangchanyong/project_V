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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateNickname(Long userId, NicknameUpdateRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(req.nickname());
        return UserResponse.from(user);
    }
}
