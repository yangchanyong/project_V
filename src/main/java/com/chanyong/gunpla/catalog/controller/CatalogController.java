package com.chanyong.gunpla.catalog.controller;

import com.chanyong.gunpla.catalog.dto.CatalogResponse;
import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.service.CatalogService;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 건프라 카탈로그 마스터 데이터 조회 API.
 * 인증 없이 누구나 접근 가능하다.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * 카탈로그 목록을 페이징 + 필터 조건으로 조회한다.
     *
     * @param request  grade / series / keyword 필터 조건
     * @param pageable 페이지 번호·사이즈 (기본 size=20, 최대 100)
     * @return 필터링된 카탈로그 목록 (페이지 메타 포함)
     */
    @GetMapping
    public PageResponse<CatalogResponse> getCatalogs(
        @ModelAttribute CatalogSearchRequest request,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return catalogService.getCatalogs(request, pageable);
    }

    /**
     * 카탈로그 단건 상세 조회.
     *
     * @param id 카탈로그 PK
     * @return 카탈로그 상세 정보
     * @throws com.chanyong.gunpla.global.exception.BusinessException CATALOG_NOT_FOUND (404)
     */
    @GetMapping("/{id}")
    public ApiResponse<CatalogResponse> getCatalog(@PathVariable Long id) {
        return ApiResponse.of(catalogService.getCatalog(id));
    }
}
