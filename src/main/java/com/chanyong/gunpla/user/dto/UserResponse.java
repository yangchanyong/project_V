package com.chanyong.gunpla.user.dto;

import com.chanyong.gunpla.user.entity.User;

import java.time.LocalDateTime;

/**
 * 유저 정보 응답 DTO.
 *
 * @param id        유저 PK
 * @param email     소셜 로그인 이메일
 * @param nickname  닉네임
 * @param provider  소셜 로그인 제공자
 * @param createdAt 가입 일시
 */
public record UserResponse(
    Long id,
    String email,
    String nickname,
    String provider,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getProvider(),
            user.getCreatedAt()
        );
    }
}
