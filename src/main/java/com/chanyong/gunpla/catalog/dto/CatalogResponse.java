package com.chanyong.gunpla.catalog.dto;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;

import java.time.LocalDate;

public record CatalogResponse(
    Long id,
    String name,
    String nameEn,
    String grade,
    String series,
    String scale,
    Integer releasePrice,
    String releasePriceCurrency,
    LocalDate releaseDate,
    String manufacturer,
    String thumbnailUrl
) {
    public static CatalogResponse from(GunplaCatalog catalog) {
        return new CatalogResponse(
            catalog.getId(),
            catalog.getName(),
            catalog.getNameEn(),
            catalog.getGrade(),
            catalog.getSeries(),
            catalog.getScale(),
            catalog.getReleasePrice(),
            catalog.getReleasePriceCurrency(),
            catalog.getReleaseDate(),
            catalog.getManufacturer(),
            catalog.getThumbnailUrl()
        );
    }
}
