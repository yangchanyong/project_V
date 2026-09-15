package com.chanyong.gunpla.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.OptionalLong;

/**
 * AWS S3 기반 스토리지 서비스 구현체.
 * SDK v2 S3Presigner를 사용하며, PUT은 5분 / GET은 1시간 Presigned URL을 생성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Duration PUT_EXPIRATION = Duration.ofSeconds(300);
    private static final Duration GET_EXPIRATION = Duration.ofHours(1);
    private static final int HTTP_NOT_FOUND = 404;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public String generatePutPresignedUrl(String s3Key, String contentType, long contentLength) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PUT_EXPIRATION)
            .putObjectRequest(r -> r
                .bucket(awsProperties.s3().bucket())
                .key(s3Key)
                .contentType(contentType)
                .contentLength(contentLength)
                .ifNoneMatch("*")
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
    public OptionalLong findObjectSize(String s3Key) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(s3Key)
                .build());
            return OptionalLong.of(response.contentLength());
        } catch (NoSuchKeyException e) {
            return OptionalLong.empty();
        } catch (S3Exception e) {
            // AIStor 등 S3-compatible 환경에서는 존재하지 않는 키에 대해 NoSuchKeyException 대신
            // generic S3Exception(404)으로 응답할 수 있다. 404만 "없음"으로 취급하고 그 외 오류는 전파한다.
            if (e.statusCode() == HTTP_NOT_FOUND) {
                return OptionalLong.empty();
            }
            throw e;
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
