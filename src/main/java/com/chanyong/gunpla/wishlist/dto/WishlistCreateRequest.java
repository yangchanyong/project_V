package com.chanyong.gunpla.wishlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WishlistCreateRequest(
    @NotNull Long catalogId,
    @NotBlank String priority,
    String memo
) {}
