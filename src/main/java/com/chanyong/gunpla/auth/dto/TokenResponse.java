package com.chanyong.gunpla.auth.dto;

public record TokenResponse(String accessToken, long expiresIn) {
}
