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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogQueryRepository catalogQueryRepository;

    public PageResponse<CatalogResponse> getCatalogs(CatalogSearchRequest req, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return PageResponse.of(catalogQueryRepository.search(req, pageable).map(CatalogResponse::from));
    }

    public CatalogResponse getCatalog(Long id) {
        return catalogRepository.findById(id)
            .map(CatalogResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND));
    }
}
