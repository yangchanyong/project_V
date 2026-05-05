package com.chanyong.gunpla.wishlist.controller;

import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import com.chanyong.gunpla.wishlist.dto.*;
import com.chanyong.gunpla.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public PageResponse<WishlistResponse> getWishlists(
        // TODO: replace with @AuthenticationPrincipal when step 6 (OAuth2/JWT) is implemented
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(required = false) String priority,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return wishlistService.getWishlists(userId, priority, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishlistCreateResponse> createWishlist(
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody @Valid WishlistCreateRequest req
    ) {
        return ApiResponse.of(wishlistService.createWishlist(userId, req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<WishlistCreateResponse> updateWishlist(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody WishlistUpdateRequest req
    ) {
        return ApiResponse.of(wishlistService.updateWishlist(userId, id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWishlist(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id
    ) {
        wishlistService.deleteWishlist(userId, id);
    }

    @PostMapping("/{id}/move-to-collection")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MoveToCollectionResponse> moveToCollection(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id,
        @RequestBody MoveToCollectionRequest req
    ) {
        return ApiResponse.of(wishlistService.moveToCollection(userId, id, req));
    }
}
