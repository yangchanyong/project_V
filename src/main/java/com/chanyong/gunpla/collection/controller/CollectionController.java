package com.chanyong.gunpla.collection.controller;

import com.chanyong.gunpla.collection.dto.*;
import com.chanyong.gunpla.collection.service.CollectionImageService;
import com.chanyong.gunpla.collection.service.CollectionService;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final CollectionImageService collectionImageService;

    @GetMapping
    public PageResponse<CollectionResponse> getCollections(
        // TODO: replace with @AuthenticationPrincipal when step 6 (OAuth2/JWT) is implemented
        @RequestHeader("X-User-Id") Long userId,
        @ModelAttribute CollectionSearchRequest req,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return collectionService.getCollections(userId, req, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CollectionCreateResponse> createCollection(
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody @Valid CollectionCreateRequest req
    ) {
        return ApiResponse.of(collectionService.createCollection(userId, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<CollectionResponse> getCollection(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id
    ) {
        return ApiResponse.of(collectionService.getCollection(userId, id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CollectionCreateResponse> updateCollection(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody CollectionUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.updateCollection(userId, id, req));
    }

    @PatchMapping("/{id}/build-status")
    public ApiResponse<BuildStatusUpdateResponse> changeStatus(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody @Valid BuildStatusUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.changeStatus(userId, id, req.buildStatus()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id
    ) {
        collectionService.deleteCollection(userId, id);
    }

    @PostMapping("/{id}/images/presigned-url")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody @Valid PresignedUrlRequest req
    ) {
        return ApiResponse.of(collectionImageService.generatePresignedUrl(userId, id, req));
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImageSaveResponse> saveImage(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody @Valid ImageSaveRequest req
    ) {
        return ApiResponse.of(collectionImageService.saveImage(userId, id, req));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @PathVariable Long imageId
    ) {
        collectionImageService.deleteImage(userId, id, imageId);
    }
}
