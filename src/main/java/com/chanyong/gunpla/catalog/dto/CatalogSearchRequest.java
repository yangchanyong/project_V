package com.chanyong.gunpla.catalog.dto;

/**
 * 카탈로그 목록 조회 필터 요청 DTO.
 *
 * @param grade   등급 필터 (완전일치, 예: HG / MG / RG / PG)
 * @param series  시리즈 필터 (부분일치)
 * @param keyword 상품명 키워드 검색 (부분일치)
 */
public record CatalogSearchRequest(String grade, String series, String keyword) {}
