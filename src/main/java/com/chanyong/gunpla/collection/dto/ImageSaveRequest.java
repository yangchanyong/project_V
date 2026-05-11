package com.chanyong.gunpla.collection.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * S3 업로드 완료 후 이미지 메타데이터 저장 요청 DTO.
 *
 * @param s3Key        Presigned URL 발급 시 서버가 생성한 S3 객체 키
 * @param displayOrder 이미지 표시 순서 (0부터 시작)
 */
public record ImageSaveRequest(
    @NotBlank String s3Key,
    int displayOrder
) {}
