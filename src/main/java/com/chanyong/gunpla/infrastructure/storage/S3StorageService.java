package com.chanyong.gunpla.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Duration PUT_EXPIRATION = Duration.ofSeconds(300);
    private static final Duration GET_EXPIRATION = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public String generatePutPresignedUrl(String s3Key, String contentType) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PUT_EXPIRATION)
            .putObjectRequest(r -> r
                .bucket(awsProperties.s3().bucket())
                .key(s3Key)
                .contentType(contentType)
            )
            .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String generateGetPresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(GET_EXPIRATION)
            .getObjectRequest(r -> r
                .bucket(awsProperties.s3().bucket())
                .key(s3Key)
            )
            .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public boolean exists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(s3Key)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public void delete(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(awsProperties.s3().bucket())
            .key(s3Key)
            .build());
    }
}
