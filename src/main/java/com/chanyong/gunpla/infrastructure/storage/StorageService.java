package com.chanyong.gunpla.infrastructure.storage;

public interface StorageService {

    String generatePutPresignedUrl(String s3Key, String contentType);

    String generateGetPresignedUrl(String s3Key);

    boolean exists(String s3Key);

    void delete(String s3Key);
}
