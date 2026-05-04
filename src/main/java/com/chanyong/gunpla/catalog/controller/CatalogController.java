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

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public PageResponse<CatalogResponse> getCatalogs(
        @ModelAttribute CatalogSearchRequest request,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return catalogService.getCatalogs(request, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<CatalogResponse> getCatalog(@PathVariable Long id) {
        return ApiResponse.of(catalogService.getCatalog(id));
    }
}
