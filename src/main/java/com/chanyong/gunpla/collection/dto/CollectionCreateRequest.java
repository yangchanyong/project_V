package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CollectionCreateRequest(
    @NotNull Long catalogId,
    @NotNull BuildStatus buildStatus,
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace,
    String memo
) {}
