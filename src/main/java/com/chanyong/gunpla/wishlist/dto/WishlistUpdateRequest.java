package com.chanyong.gunpla.wishlist.dto;

/**
 * 위시리스트 수정 요청 DTO.
 *
 * @param priority 수정할 우선순위 (null이면 기존 값 유지)
 * @param memo     수정할 메모 (null이면 기존 값 유지)
 */
public record WishlistUpdateRequest(String priority, String memo) {}
