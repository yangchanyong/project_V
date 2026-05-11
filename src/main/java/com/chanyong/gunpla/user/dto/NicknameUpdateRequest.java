package com.chanyong.gunpla.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 수정 요청 DTO.
 *
 * @param nickname 변경할 닉네임 (필수, 최대 50자)
 */
public record NicknameUpdateRequest(
    @NotBlank @Size(max = 50) String nickname
) {
}
