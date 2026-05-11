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

/**
 * 위시리스트 관리 API.
 * 모든 엔드포인트는 JWT 인증이 필요하며, 본인 위시리스트에만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 내 위시리스트 목록을 조회한다.
     *
     * @param principal 현재 로그인 유저
     * @param priority  우선순위 필터 (null이면 전체)
     * @param pageable  페이지 정보 (기본 size=20, 최대 100)
     * @return 위시리스트 목록 (카탈로그 정보 포함)
     */
    @GetMapping
    public PageResponse<WishlistResponse> getWishlists(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String priority,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return wishlistService.getWishlists(principal.getId(), priority, pageable);
    }

    /**
     * 위시리스트에 카탈로그를 추가한다.
     * 같은 카탈로그가 이미 있으면 409 WISHLIST_ALREADY_EXISTS를 반환한다.
     *
     * @param principal 현재 로그인 유저
     * @param req       카탈로그 ID, 우선순위, 메모
     * @return 생성된 위시리스트 ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishlistCreateResponse> createWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid WishlistCreateRequest req
    ) {
        return ApiResponse.of(wishlistService.createWishlist(principal.getId(), req));
    }

    /**
     * 위시리스트 우선순위·메모를 수정한다.
     *
     * @param principal 현재 로그인 유저
     * @param id        위시리스트 PK
     * @param req       수정할 우선순위, 메모
     * @return 수정된 위시리스트 ID
     */
    @PatchMapping("/{id}")
    public ApiResponse<WishlistCreateResponse> updateWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody WishlistUpdateRequest req
    ) {
        return ApiResponse.of(wishlistService.updateWishlist(principal.getId(), id, req));
    }

    /**
     * 위시리스트 항목을 삭제한다.
     *
     * @param principal 현재 로그인 유저
     * @param id        위시리스트 PK
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWishlist(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        wishlistService.deleteWishlist(principal.getId(), id);
    }

    /**
     * 위시리스트 항목을 컬렉션으로 이동한다.
     * 하나의 트랜잭션 안에서 컬렉션 생성 → 위시리스트 삭제가 원자적으로 처리된다.
     *
     * @param principal 현재 로그인 유저
     * @param id        위시리스트 PK
     * @param req       구매 정보 (선택)
     * @return 생성된 컬렉션 ID
     */
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
