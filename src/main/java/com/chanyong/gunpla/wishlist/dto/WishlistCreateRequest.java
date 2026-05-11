package com.chanyong.gunpla.wishlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 위시리스트 등록 요청 DTO.
 *
 * @param catalogId 추가할 카탈로그 PK (필수)
 * @param priority  우선순위 (필수, 예: HIGH / MEDIUM / LOW)
 * @param memo      메모 (선택)
 */
public record WishlistCreateRequest(
    @NotNull Long catalogId,
    @NotBlank String priority,
    String memo
) {}
