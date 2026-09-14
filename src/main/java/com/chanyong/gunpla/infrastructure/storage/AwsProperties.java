package com.chanyong.gunpla.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3-compatible Object Storage 접속 설정.
 * ARK MinIO AIStor 등 S3-compatible endpoint 지원을 위해 endpoint / presignEndpoint /
 * pathStyleAccess 를 함께 받는다.
 */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    String region,
    S3 s3,
    Credentials credentials
) {
    /**
     * @param bucket           대상 버킷명
     * @param endpoint         서버 내부(SDK) 통신용 S3 API endpoint. null/blank 이면 AWS 기본 endpoint 사용
     * @param presignEndpoint  Presigned URL 발급용 외부 endpoint. 클라이언트가 직접 접근할 공개 도메인
     * @param pathStyleAccess  path-style URL 강제 사용 여부 (MinIO 등 S3-compatible 환경 필수)
     */
    public record S3(
        String bucket,
        String endpoint,
        String presignEndpoint,
        boolean pathStyleAccess
    ) {}

    public record Credentials(String accessKey, String secretKey) {}
}
