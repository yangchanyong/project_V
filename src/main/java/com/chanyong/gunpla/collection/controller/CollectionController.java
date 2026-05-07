package com.chanyong.gunpla.collection.controller;

import com.chanyong.gunpla.collection.dto.*;
import com.chanyong.gunpla.collection.service.CollectionImageService;
import com.chanyong.gunpla.collection.service.CollectionService;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.ratelimit.RateLimited;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final CollectionImageService collectionImageService;

    @GetMapping
    public PageResponse<CollectionResponse> getCollections(
        @AuthenticationPrincipal UserPrincipal principal,
        @ModelAttribute CollectionSearchRequest req,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return collectionService.getCollections(principal.getId(), req, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CollectionCreateResponse> createCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid CollectionCreateRequest req
    ) {
        return ApiResponse.of(collectionService.createCollection(principal.getId(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<CollectionResponse> getCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        return ApiResponse.of(collectionService.getCollection(principal.getId(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CollectionCreateResponse> updateCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody CollectionUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.updateCollection(principal.getId(), id, req));
    }

    @PatchMapping("/{id}/build-status")
    public ApiResponse<BuildStatusUpdateResponse> changeStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid BuildStatusUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.changeStatus(principal.getId(), id, req.buildStatus()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        collectionService.deleteCollection(principal.getId(), id);
    }

    @RateLimited(limit = 20)
    @PostMapping("/{id}/images/presigned-url")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid PresignedUrlRequest req
    ) {
        return ApiResponse.of(collectionImageService.generatePresignedUrl(principal.getId(), id, req));
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImageSaveResponse> saveImage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid ImageSaveRequest req
    ) {
        return ApiResponse.of(collectionImageService.saveImage(principal.getId(), id, req));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @PathVariable Long imageId
    ) {
        collectionImageService.deleteImage(principal.getId(), id, imageId);
    }
}
