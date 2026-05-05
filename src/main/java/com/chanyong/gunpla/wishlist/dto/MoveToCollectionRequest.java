package com.chanyong.gunpla.wishlist.dto;

import java.time.LocalDate;

public record MoveToCollectionRequest(
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace
) {}
