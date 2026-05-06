package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.CollectionImage;
import com.chanyong.gunpla.collection.entity.UserCollection;
import com.chanyong.gunpla.infrastructure.storage.StorageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CollectionResponse(
    Long id,
    CatalogSummary catalog,
    BuildStatus buildStatus,
    Integer purchasePrice,
    String purchaseCurrency,
    LocalDate purchaseDate,
    String purchasePlace,
    String memo,
    List<CollectionImageSummary> images,
    LocalDateTime createdAt
) {
    public static CollectionResponse from(UserCollection c, List<CollectionImage> images, StorageService storageService) {
        return new CollectionResponse(
            c.getId(),
            CatalogSummary.from(c.getCatalog()),
            c.getBuildStatus(),
            c.getPurchasePrice(),
            c.getPurchaseCurrency(),
            c.getPurchaseDate(),
            c.getPurchasePlace(),
            c.getMemo(),
            images.stream()
                .map(img -> CollectionImageSummary.from(img, storageService.generateGetPresignedUrl(img.getS3Key())))
                .toList(),
            c.getCreatedAt()
        );
    }
}
