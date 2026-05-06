package com.chanyong.gunpla.infrastructure.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(AwsProperties props) {
        return S3Client.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsProperties props) {
        return S3Presigner.builder()
            .region(Region.of(props.region()))
            .credentialsProvider(credentialsProvider(props))
            .build();
    }

    private StaticCredentialsProvider credentialsProvider(AwsProperties props) {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                props.credentials().accessKey(),
                props.credentials().secretKey()
            )
        );
    }
}
