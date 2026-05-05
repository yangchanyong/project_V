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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;
    private final CollectionRepository collectionRepository;

    public PageResponse<WishlistResponse> getWishlists(Long userId, String priority, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Page<Wishlist> page = StringUtils.hasText(priority)
            ? wishlistRepository.findByUserIdAndPriority(userId, priority, pageable)
            : wishlistRepository.findByUserId(userId, pageable);

        return PageResponse.of(page.map(WishlistResponse::from));
    }

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

    @Transactional
    public WishlistCreateResponse updateWishlist(Long userId, Long id, WishlistUpdateRequest req) {
        Wishlist wishlist = findOwnedWishlist(id, userId);
        wishlist.update(req.priority(), req.memo());
        return new WishlistCreateResponse(wishlist.getId());
    }

    @Transactional
    public void deleteWishlist(Long userId, Long id) {
        Wishlist wishlist = findOwnedWishlist(id, userId);
        wishlistRepository.delete(wishlist);
    }

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

    private Wishlist findOwnedWishlist(Long id, Long userId) {
        Wishlist wishlist = wishlistRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.WISHLIST_NOT_FOUND));
        if (!wishlist.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WISHLIST_ACCESS_DENIED);
        }
        return wishlist;
    }
}
