package com.chanyong.gunpla.wishlist.dto;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;

public record WishlistCatalogSummary(Long id, String name, String grade, String thumbnailUrl) {

    public static WishlistCatalogSummary from(GunplaCatalog catalog) {
        return new WishlistCatalogSummary(
            catalog.getId(),
            catalog.getName(),
            catalog.getGrade(),
            catalog.getThumbnailUrl()
        );
    }
}
