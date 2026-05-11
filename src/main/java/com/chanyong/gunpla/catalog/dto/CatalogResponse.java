package com.chanyong.gunpla.catalog.dto;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;

import java.time.LocalDate;

/**
 * 카탈로그 단건 응답 DTO.
 * {@link com.chanyong.gunpla.catalog.entity.GunplaCatalog} 엔티티를 클라이언트에 노출하는 형태로 변환한다.
 */
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
