package com.chanyong.gunpla.collection.dto;

public record PresignedUrlResponse(
    String presignedUrl,
    String s3Key,
    int expiresIn
) {}
