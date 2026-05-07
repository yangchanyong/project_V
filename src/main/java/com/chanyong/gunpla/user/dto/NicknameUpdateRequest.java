package com.chanyong.gunpla.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
    @NotBlank @Size(max = 50) String nickname
) {
}
