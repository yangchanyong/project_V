package com.chanyong.gunpla.collection.dto;

import java.time.LocalDate;

public record CollectionUpdateRequest(
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace,
    String memo
) {}
