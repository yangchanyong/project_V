package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.CollectionImage;

public record CollectionImageSummary(Long id, String url, int displayOrder) {

    public static CollectionImageSummary from(CollectionImage image, String presignedUrl) {
        return new CollectionImageSummary(image.getId(), presignedUrl, image.getDisplayOrder());
    }
}
