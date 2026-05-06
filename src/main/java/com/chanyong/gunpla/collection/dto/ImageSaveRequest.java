package com.chanyong.gunpla.collection.dto;

import jakarta.validation.constraints.NotBlank;

public record ImageSaveRequest(
    @NotBlank String s3Key,
    int displayOrder
) {}
