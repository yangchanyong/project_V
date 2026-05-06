package com.chanyong.gunpla.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresignedUrlRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @Positive long fileSize
) {}
