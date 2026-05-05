package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.CollectionImage;

public record CollectionImageSummary(Long id, String url, int displayOrder) {

    // url 필드는 현재 s3Key를 그대로 사용. 5단계에서 Presigned URL로 전환 예정.
    public static CollectionImageSummary from(CollectionImage image) {
        return new CollectionImageSummary(image.getId(), image.getS3Key(), image.getDisplayOrder());
    }
}
