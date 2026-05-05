package com.chanyong.gunpla.wishlist.dto;

import com.chanyong.gunpla.wishlist.entity.Wishlist;

import java.time.LocalDateTime;

public record WishlistResponse(
    Long id,
    WishlistCatalogSummary catalog,
    String priority,
    String memo,
    LocalDateTime createdAt
) {
    public static WishlistResponse from(Wishlist w) {
        return new WishlistResponse(
            w.getId(),
            WishlistCatalogSummary.from(w.getCatalog()),
            w.getPriority(),
            w.getMemo(),
            w.getCreatedAt()
        );
    }
}
