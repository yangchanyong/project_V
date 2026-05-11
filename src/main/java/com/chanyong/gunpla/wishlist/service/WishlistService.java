package com.chanyong.gunpla.wishlist.service;

import com.chanyong.gunpla.catalog.repository.CatalogRepository;
import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.UserCollection;
import com.chanyong.gunpla.collection.repository.CollectionRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.PageResponse;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import com.chanyong.gunpla.wishlist.dto.*;
import com.chanyong.gunpla.wishlist.entity.Wishlist;
import com.chanyong.gunpla.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 위시리스트 CRUD 및 컬렉션 이동 서비스.
 * 모든 쓰기 작업은 {@link #findOwnedWishlist}로 소유권을 먼저 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;
    private final CollectionRepository collectionRepository;

    /**
     * 내 위시리스트 목록을 조회한다.
     * priority 파라미터가 있으면 필터링하고, 없으면 전체를 반환한다.
     * catalog는 @EntityGraph로 함께 로딩한다 (N+1 방지).
     *
     * @param userId   로그인 유저 ID
     * @param priority 우선순위 필터 (null이면 전체)
     * @param pageable 페이지 정보
     * @return 페이징된 위시리스트 목록
     */
    public PageResponse<WishlistResponse> getWishlists(Long userId, String priority, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Page<Wishlist> page = StringUtils.hasText(priority)
            ? wishlistRepository.findByUserIdAndPriority(userId, priority, pageable)
            : wishlistRepository.findByUserId(userId, pageable);

        return PageResponse.of(page.map(WishlistResponse::from));
    }

    /**
     * 위시리스트에 카탈로그를 추가한다.
     * (userId, catalogId) 조합이 이미 존재하면 WISHLIST_ALREADY_EXISTS(409) 예외를 던진다.
     *
     * @param userId 로그인 유저 ID
     * @param req    카탈로그 ID, 우선순위, 메모
     * @return 생성된 위시리스트 ID
     */
    @Transactional
    public WishlistCreateResponse createWishlist(Long userId, WishlistCreateRequest req) {
        if (wishlistRepository.existsByUserIdAndCatalogId(userId, req.catalogId())) {
            throw new BusinessException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var catalog = catalogRepository.findById(req.catalogId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND));

        Wishlist wishlist = Wishlist.builder()
            .user(user)
            .catalog(catalog)
            .priority(req.priority())
            .memo(req.memo())
            .build();

        return new WishlistCreateResponse(wishlistRepository.save(wishlist).getId());
    }

    /**
     * 위시리스트 우선순위·메모를 수정한다.
     *
     * @param userId 로그인 유저 ID
     * @param id     위시리스트 PK
     * @param req    수정할 우선순위, 메모
     * @return 수정된 위시리스트 ID
     */
    @Transactional
    public WishlistCreateResponse updateWishlist(Long userId, Long id, WishlistUpdateRequest req) {
        Wishlist wishlist = findOwnedWishlist(id, userId);
        wishlist.update(req.priority(), req.memo());
        return new WishlistCreateResponse(wishlist.getId());
    }

    /**
     * 위시리스트 항목을 삭제한다.
     *
     * @param userId 로그인 유저 ID
     * @param id     위시리스트 PK
     */
    @Transactional
    public void deleteWishlist(Long userId, Long id) {
        Wishlist wishlist = findOwnedWishlist(id, userId);
        wishlistRepository.delete(wishlist);
    }

    /**
     * 위시리스트 항목을 컬렉션으로 이동한다.
     * 하나의 트랜잭션에서 컬렉션 생성 후 위시리스트를 삭제하므로 원자적으로 처리된다.
     *
     * @param userId 로그인 유저 ID
     * @param id     위시리스트 PK
     * @param req    컬렉션에 넣을 구매 정보
     * @return 생성된 컬렉션 ID
     */
    @Transactional
    public MoveToCollectionResponse moveToCollection(Long userId, Long id, MoveToCollectionRequest req) {
        Wishlist wishlist = findOwnedWishlist(id, userId);
        var catalog = catalogRepository.findById(wishlist.getCatalog().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND));

        UserCollection collection = UserCollection.builder()
            .user(wishlist.getUser())
            .catalog(catalog)
            .buildStatus(BuildStatus.UNBUILT)
            .purchasePrice(req.purchasePrice())
            .purchaseCurrency(req.purchaseCurrency())
            .purchaseDate(req.purchaseDate())
            .purchasePlace(req.purchasePlace())
            .build();

        UserCollection saved = collectionRepository.save(collection);
        wishlistRepository.delete(wishlist);

        return new MoveToCollectionResponse(saved.getId());
    }

    /**
     * 위시리스트를 조회하고 소유권을 검증한다.
     *
     * @param id     위시리스트 PK
     * @param userId 소유자 유저 ID
     * @return 검증된 위시리스트 엔티티
     * @throws com.chanyong.gunpla.global.exception.BusinessException WISHLIST_NOT_FOUND(404), WISHLIST_ACCESS_DENIED(403)
     */
    private Wishlist findOwnedWishlist(Long id, Long userId) {
        Wishlist wishlist = wishlistRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.WISHLIST_NOT_FOUND));
        if (!wishlist.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WISHLIST_ACCESS_DENIED);
        }
        return wishlist;
    }
}
