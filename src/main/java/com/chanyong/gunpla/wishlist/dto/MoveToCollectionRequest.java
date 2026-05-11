package com.chanyong.gunpla.wishlist.dto;

import java.time.LocalDate;

/**
 * 위시리스트 → 컬렉션 이동 요청 DTO.
 * 모든 필드는 선택 사항이다.
 *
 * @param purchasePrice    구매 가격
 * @param purchaseCurrency 통화 코드 (예: KRW, JPY)
 * @param purchaseDate     구매 일자
 * @param purchasePlace    구매처
 */
public record MoveToCollectionRequest(
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace
) {}
