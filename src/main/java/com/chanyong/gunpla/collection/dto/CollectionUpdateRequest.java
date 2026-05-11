package com.chanyong.gunpla.collection.dto;

import java.time.LocalDate;

/**
 * 컬렉션 수정 요청 DTO. 모든 필드는 선택 사항이며 null이면 기존 값을 덮어쓴다.
 *
 * @param purchasePrice    구매 가격
 * @param purchaseCurrency 통화 코드
 * @param purchaseDate     구매 일자
 * @param purchasePlace    구매처
 * @param memo             메모
 */
public record CollectionUpdateRequest(
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace,
    String memo
) {}
