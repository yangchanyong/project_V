package com.chanyong.gunpla.infrastructure.storage;

import java.util.OptionalLong;

/**
 * 파일 스토리지 서비스 인터페이스.
 * 운영 환경에서는 {@link S3StorageService}가 주입된다.
 */
public interface StorageService {

    /**
     * S3에 파일을 업로드하기 위한 Presigned PUT URL을 생성한다. 유효 시간은 5분이다.
     * Content-Length를 정확한 값으로 서명해 신고된 크기와 실제 전송 크기의 불일치를 방지하고,
     * If-None-Match: * 를 서명해 동일 키에 대한 덮어쓰기(재업로드)를 방지한다.
     *
     * @param s3Key         저장할 S3 객체 키
     * @param contentType   업로드할 파일의 MIME 타입
     * @param contentLength 업로드할 파일의 정확한 바이트 크기
     * @return 클라이언트가 직접 PUT 요청을 보낼 Presigned URL
     */
    String generatePutPresignedUrl(String s3Key, String contentType, long contentLength);

    /**
     * S3 파일을 임시로 조회하기 위한 Presigned GET URL을 생성한다. 유효 시간은 1시간이다.
     *
     * @param s3Key 조회할 S3 객체 키
     * @return 클라이언트가 직접 GET 요청을 보낼 Presigned URL
     */
    String generateGetPresignedUrl(String s3Key);

    /**
     * S3에 해당 키의 객체가 존재하는지 확인하고, 존재하면 실제 크기(바이트)를 반환한다 (HeadObject 1회 호출).
     *
     * @param s3Key 확인할 S3 객체 키
     * @return 존재하면 실제 contentLength를 담은 값, 존재하지 않으면 빈 값
     */
    OptionalLong findObjectSize(String s3Key);

    /**
     * S3에서 객체를 삭제한다.
     *
     * @param s3Key 삭제할 S3 객체 키
     */
    void delete(String s3Key);
}
