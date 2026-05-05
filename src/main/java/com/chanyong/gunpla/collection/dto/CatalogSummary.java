package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;

public record CatalogSummary(Long id, String name, String grade, String thumbnailUrl) {

    public static CatalogSummary from(GunplaCatalog catalog) {
        return new CatalogSummary(
            catalog.getId(),
            catalog.getName(),
            catalog.getGrade(),
            catalog.getThumbnailUrl()
        );
    }
}
