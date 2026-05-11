package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;

/**
 * 컬렉션 목록 조회 필터 요청 DTO.
 *
 * @param buildStatus 빌드 상태 필터 (null이면 전체)
 * @param grade       카탈로그 등급 필터 (null이면 전체)
 */
public record CollectionSearchRequest(BuildStatus buildStatus, String grade) {}
