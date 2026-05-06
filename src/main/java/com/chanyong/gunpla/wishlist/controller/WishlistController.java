package com.chanyong.gunpla.wishlist.controller;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import com.chanyong.gunpla.wishlist.dto.*;
import com.chanyong.gunpla.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public PageResponse<WishlistResponse> getWishlists(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String priority,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return wishlistService.getWishlists(principal.getId(), priority, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishlistCreateResponse> createWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid WishlistCreateRequest req
    ) {
        return ApiResponse.of(wishlistService.createWishlist(principal.getId(), req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<WishlistCreateResponse> updateWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody WishlistUpdateRequest req
    ) {
        return ApiResponse.of(wishlistService.updateWishlist(principal.getId(), id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        wishlistService.deleteWishlist(principal.getId(), id);
    }

    @PostMapping("/{id}/move-to-collection")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MoveToCollectionResponse> moveToCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody MoveToCollectionRequest req
    ) {
        return ApiResponse.of(wishlistService.moveToCollection(principal.getId(), id, req));
    }
}
