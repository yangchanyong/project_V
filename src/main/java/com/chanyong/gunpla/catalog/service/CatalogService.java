package com.chanyong.gunpla.catalog.service;

import com.chanyong.gunpla.catalog.dto.CatalogResponse;
import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.repository.CatalogQueryRepository;
import com.chanyong.gunpla.catalog.repository.CatalogRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 건프라 카탈로그 마스터 데이터 조회 서비스.
 * 카탈로그는 관리자가 Flyway 마이그레이션으로만 관리하며 애플리케이션에서 직접 수정하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogQueryRepository catalogQueryRepository;

    /**
     * 필터 조건으로 카탈로그 목록을 조회한다.
     * size 최대값(100)을 초과하면 INVALID_INPUT 예외를 던진다.
     *
     * @param req      grade / series / keyword 필터 조건
     * @param pageable 페이지 정보
     * @return 페이징된 카탈로그 응답 목록
     */
    public PageResponse<CatalogResponse> getCatalogs(CatalogSearchRequest req, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return PageResponse.of(catalogQueryRepository.search(req, pageable).map(CatalogResponse::from));
    }

    /**
     * 카탈로그 단건을 조회한다.
     *
     * @param id 카탈로그 PK
     * @return 카탈로그 응답 DTO
     * @throws BusinessException CATALOG_NOT_FOUND (404)
     */
    public CatalogResponse getCatalog(Long id) {
        return catalogRepository.findById(id)
            .map(CatalogResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND));
    }
}
