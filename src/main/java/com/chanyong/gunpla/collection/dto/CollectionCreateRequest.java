package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 컬렉션 등록 요청 DTO.
 *
 * @param catalogId       등록할 카탈로그 PK (필수)
 * @param buildStatus     초기 빌드 상태 (필수)
 * @param purchasePrice   구매 가격
 * @param purchaseCurrency 통화 코드 (예: KRW, JPY)
 * @param purchaseDate    구매 일자
 * @param purchasePlace   구매처
 * @param memo            메모
 */
public record CollectionCreateRequest(
    @NotNull Long catalogId,
    @NotNull BuildStatus buildStatus,
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace,
    String memo
) {}
