package com.chanyong.gunpla.catalog.controller;

import com.chanyong.gunpla.catalog.dto.CatalogResponse;
import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.service.CatalogService;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 건프라 카탈로그 마스터 데이터 조회 API.
 * 인증 없이 누구나 접근 가능하다.
 */
@Tag(name = "Catalog", description = "건프라 카탈로그 마스터 데이터 조회 (인증 불필요)")
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
    @Operation(summary = "카탈로그 목록 조회", description = "grade / series / keyword 필터와 페이징을 지원한다. size 최대 100.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public PageResponse<CatalogResponse> getCatalogs(
        @Parameter(description = "등급 필터 (HG, MG, RG, PG 등 완전일치)") @ModelAttribute CatalogSearchRequest request,
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
    @Operation(summary = "카탈로그 단건 조회", description = "카탈로그 PK로 상세 정보를 조회한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카탈로그 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<CatalogResponse> getCatalog(
        @Parameter(description = "카탈로그 PK") @PathVariable Long id
    ) {
        return ApiResponse.of(catalogService.getCatalog(id));
    }
}
