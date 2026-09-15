package com.chanyong.gunpla.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Presigned PUT 서명 대상 헤더(Content-Length / If-None-Match)와
 * HeadObject 기반 실측 크기 조회 동작을 검증한다.
 *
 * S3Presigner는 순수 로컬 서명 계산만 수행하고 실제 네트워크 호출을 하지 않으므로
 * 실제 인스턴스를 생성해 서명 결과(X-Amz-SignedHeaders)를 직접 확인한다.
 * S3Client는 HeadObject 호출이 실제 네트워크를 타므로 Mockito mock을 사용한다.
 */
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        var credentials = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test-access-key", "test-secret-key")
        );
        S3Presigner realPresigner = S3Presigner.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .build();

        AwsProperties awsProperties = new AwsProperties(
            "us-east-1",
            new AwsProperties.S3(BUCKET, null, null, true),
            new AwsProperties.Credentials("test-access-key", "test-secret-key")
        );

        s3StorageService = new S3StorageService(s3Client, realPresigner, awsProperties);
    }

    // ──────────────────────────────────────────
    // generatePutPresignedUrl — Signed Headers 검증
    // ──────────────────────────────────────────

    @Test
    void generatePutPresignedUrl_contentLength가_SignedHeader에_포함된다() {
        String url = s3StorageService.generatePutPresignedUrl("test-key", "image/jpeg", 1024L);

        String signedHeaders = extractSignedHeaders(url);

        assertThat(signedHeaders).contains("content-length");
    }

    @Test
    void generatePutPresignedUrl_ifNoneMatch가_SignedHeader에_포함된다() {
        String url = s3StorageService.generatePutPresignedUrl("test-key", "image/jpeg", 1024L);

        String signedHeaders = extractSignedHeaders(url);

        assertThat(signedHeaders).contains("if-none-match");
    }

    @Test
    void generatePutPresignedUrl_contentType도_여전히_SignedHeader에_포함된다() {
        String url = s3StorageService.generatePutPresignedUrl("test-key", "image/jpeg", 1024L);

        String signedHeaders = extractSignedHeaders(url);

        assertThat(signedHeaders).contains("content-type");
    }

    @Test
    void generatePutPresignedUrl_세_헤더_모두_동시에_서명된다() {
        String url = s3StorageService.generatePutPresignedUrl("test-key", "image/jpeg", 1024L);

        String signedHeaders = extractSignedHeaders(url);

        assertThat(signedHeaders.split(";"))
            .containsExactlyInAnyOrder("content-length", "content-type", "host", "if-none-match");
    }

    private String extractSignedHeaders(String url) {
        String marker = "X-Amz-SignedHeaders=";
        int idx = url.indexOf(marker);
        assertThat(idx).as("URL에 X-Amz-SignedHeaders 파라미터가 있어야 함").isGreaterThanOrEqualTo(0);
        int end = url.indexOf('&', idx);
        String raw = end > 0
            ? url.substring(idx + marker.length(), end)
            : url.substring(idx + marker.length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    // ──────────────────────────────────────────
    // findObjectSize — HeadObject 기반 실측 크기 조회
    // ──────────────────────────────────────────

    @Test
    void findObjectSize_정상_객체는_실제_크기를_반환한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willReturn(HeadObjectResponse.builder().contentLength(1234L).build());

        OptionalLong result = s3StorageService.findObjectSize("test-key");

        assertThat(result).isPresent();
        assertThat(result.getAsLong()).isEqualTo(1234L);
    }

    @Test
    void findObjectSize_NoSuchKeyException이면_빈값을_반환한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(NoSuchKeyException.builder().message("not found").build());

        OptionalLong result = s3StorageService.findObjectSize("missing-key");

        assertThat(result).isEmpty();
    }

    @Test
    void findObjectSize_404_S3Exception이면_빈값을_반환한다() {
        // AIStor 등 S3-compatible 환경이 NoSuchKeyException 대신 generic 404 S3Exception을 던지는 경우 대비
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(404).message("not found").build());

        OptionalLong result = s3StorageService.findObjectSize("missing-key");

        assertThat(result).isEmpty();
    }

    @Test
    void findObjectSize_404가_아닌_S3Exception은_그대로_전파한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(500).message("internal error").build());

        assertThatThrownBy(() -> s3StorageService.findObjectSize("test-key"))
            .isInstanceOf(S3Exception.class)
            .extracting(e -> ((S3Exception) e).statusCode())
            .isEqualTo(500);
    }

    @Test
    void findObjectSize는_HeadObject를_1회만_호출한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willReturn(HeadObjectResponse.builder().contentLength(1024L).build());

        s3StorageService.findObjectSize("test-key");

        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }
}
