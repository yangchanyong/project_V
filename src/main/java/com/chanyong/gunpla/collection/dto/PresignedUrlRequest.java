package com.chanyong.gunpla.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * S3 Presigned URL 발급 요청 DTO.
 *
 * @param fileName    업로드할 파일명
 * @param contentType MIME 타입 (허용: image/jpeg, image/png, image/webp)
 * @param fileSize    파일 크기 (바이트, 최대 10MB)
 */
public record PresignedUrlRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @Positive long fileSize
) {}
