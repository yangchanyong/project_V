package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.CollectionImage;
import com.chanyong.gunpla.collection.entity.UserCollection;

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
    public static CollectionResponse from(UserCollection c, List<CollectionImage> images) {
        return new CollectionResponse(
            c.getId(),
            CatalogSummary.from(c.getCatalog()),
            c.getBuildStatus(),
            c.getPurchasePrice(),
            c.getPurchaseCurrency(),
            c.getPurchaseDate(),
            c.getPurchasePlace(),
            c.getMemo(),
            images.stream().map(CollectionImageSummary::from).toList(),
            c.getCreatedAt()
        );
    }
}
