package com.chanyong.gunpla.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    String region,
    S3 s3,
    Credentials credentials
) {
    public record S3(String bucket) {}

    public record Credentials(String accessKey, String secretKey) {}
}
