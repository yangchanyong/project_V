package com.chanyong.gunpla.infrastructure.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3-compatible Object Storage 클라이언트 설정.
 *
 * S3Client: 서버 내부에서 S3 API를 호출하는 통신용 (HeadObject/DeleteObject 등)
 *   → aws.s3.endpoint 사용 (예: http://ark-object-storage:9000)
 *
 * S3Presigner: 클라이언트가 직접 접근할 Presigned URL 생성용
 *   → aws.s3.presign-endpoint 사용 (예: https://storage.vibe.chanyongyang.com)
 *   내부 hostname이 URL에 노출되지 않도록 반드시 분리 유지.
 *
 * pathStyleAccess: MinIO 등 S3-compatible 백엔드는 virtual-hosted-style을 지원하지 않는 경우가 많아 path-style 강제.
 * endpoint 값이 비어있으면 override를 생략해 AWS SDK 기본(리전 기반) endpoint를 사용한다.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(AwsProperties props) {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .serviceConfiguration(serviceConfiguration(props));
        applyEndpointOverride(builder, props.s3().endpoint());
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsProperties props) {
        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .serviceConfiguration(serviceConfiguration(props));
        applyEndpointOverride(builder, props.s3().presignEndpoint());
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider(AwsProperties props) {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                props.credentials().accessKey(),
                props.credentials().secretKey()
            )
        );
    }

    private S3Configuration serviceConfiguration(AwsProperties props) {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(props.s3().pathStyleAccess())
            .build();
    }

    private void applyEndpointOverride(S3ClientBuilder builder, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }

    private void applyEndpointOverride(S3Presigner.Builder builder, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }
}
