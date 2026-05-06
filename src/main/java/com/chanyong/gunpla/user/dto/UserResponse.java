package com.chanyong.gunpla.user.dto;

import com.chanyong.gunpla.user.entity.User;

import java.time.LocalDateTime;

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
