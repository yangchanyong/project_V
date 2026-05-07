package com.chanyong.gunpla.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 입력입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),

    // 컬렉션
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "허용되지 않은 빌드 상태 전이입니다."),
    COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND", "컬렉션을 찾을 수 없습니다."),
    COLLECTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COLLECTION_ACCESS_DENIED", "컬렉션에 대한 접근 권한이 없습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // 카탈로그
    CATALOG_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_NOT_FOUND", "카탈로그를 찾을 수 없습니다."),

    // 위시리스트
    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WISHLIST_NOT_FOUND", "위시리스트 항목을 찾을 수 없습니다."),
    WISHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "WISHLIST_ALREADY_EXISTS", "이미 위시리스트에 존재하는 항목입니다."),
    WISHLIST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "WISHLIST_ACCESS_DENIED", "위시리스트에 대한 접근 권한이 없습니다."),

    // 컬렉션 이미지
    COLLECTION_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "COLLECTION_IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다."),
    FILE_UPLOAD_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "FILE_UPLOAD_VALIDATION_FAILED", "파일 형식 또는 크기가 유효하지 않습니다."),

    // 인증
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않거나 만료된 Refresh Token입니다."),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "요청 한도를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
