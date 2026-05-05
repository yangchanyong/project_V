package com.chanyong.gunpla.wishlist.service;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.catalog.repository.CatalogRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @InjectMocks private WishlistService wishlistService;

    @Mock private WishlistRepository wishlistRepository;
    @Mock private UserRepository userRepository;
    @Mock private CatalogRepository catalogRepository;
    @Mock private CollectionRepository collectionRepository;

    @Test
    void createWishlist_정상_생성() {
        User user = mock(User.class);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        Wishlist saved = mock(Wishlist.class);
        WishlistCreateRequest req = new WishlistCreateRequest(10L, "HIGH", "메모");

        given(wishlistRepository.existsByUserIdAndCatalogId(1L, 10L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(catalogRepository.findById(10L)).willReturn(Optional.of(catalog));
        given(wishlistRepository.save(any())).willReturn(saved);
        given(saved.getId()).willReturn(1L);

        WishlistCreateResponse result = wishlistService.createWishlist(1L, req);

        assertThat(result.id()).isEqualTo(1L);
        verify(wishlistRepository).save(any());
    }

    @Test
    void createWishlist_중복_409예외() {
        WishlistCreateRequest req = new WishlistCreateRequest(10L, "HIGH", null);
        given(wishlistRepository.existsByUserIdAndCatalogId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> wishlistService.createWishlist(1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.WISHLIST_ALREADY_EXISTS);
    }

    @Test
    void updateWishlist_정상_수정() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        Wishlist wishlist = Wishlist.builder().user(user).catalog(mock(GunplaCatalog.class))
            .priority("HIGH").memo("기존메모").build();
        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));

        WishlistCreateResponse result = wishlistService.updateWishlist(1L, 1L,
            new WishlistUpdateRequest("LOW", "수정메모"));

        assertThat(wishlist.getPriority()).isEqualTo("LOW");
        assertThat(wishlist.getMemo()).isEqualTo("수정메모");
    }

    @Test
    void deleteWishlist_소유권검증_통과() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        Wishlist wishlist = Wishlist.builder().user(user).catalog(mock(GunplaCatalog.class))
            .priority("HIGH").build();
        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));

        wishlistService.deleteWishlist(1L, 1L);

        verify(wishlistRepository).delete(wishlist);
    }

    @Test
    void deleteWishlist_다른유저_403예외() {
        User otherUser = mock(User.class);
        given(otherUser.getId()).willReturn(99L);
        Wishlist wishlist = Wishlist.builder().user(otherUser).catalog(mock(GunplaCatalog.class))
            .priority("HIGH").build();
        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> wishlistService.deleteWishlist(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.WISHLIST_ACCESS_DENIED);
    }

    @Test
    void moveToCollection_정상_이동() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        given(catalog.getId()).willReturn(10L);
        Wishlist wishlist = Wishlist.builder().user(user).catalog(catalog).priority("HIGH").build();
        UserCollection savedCollection = mock(UserCollection.class);
        given(savedCollection.getId()).willReturn(5L);

        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));
        given(catalogRepository.findById(10L)).willReturn(Optional.of(catalog));
        given(collectionRepository.save(any())).willReturn(savedCollection);

        MoveToCollectionResponse result = wishlistService.moveToCollection(1L, 1L,
            new MoveToCollectionRequest(null, "JPY", null, null));

        assertThat(result.collectionId()).isEqualTo(5L);
        verify(collectionRepository).save(any());
        verify(wishlistRepository).delete(wishlist);
    }

    @Test
    void moveToCollection_catalog없음_404예외() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        given(catalog.getId()).willReturn(10L);
        Wishlist wishlist = Wishlist.builder().user(user).catalog(catalog).priority("HIGH").build();

        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));
        given(catalogRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.moveToCollection(1L, 1L,
            new MoveToCollectionRequest(null, null, null, null)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CATALOG_NOT_FOUND);
    }

    @Test
    void moveToCollection_저장실패시_wishlist미삭제() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        given(catalog.getId()).willReturn(10L);
        Wishlist wishlist = Wishlist.builder().user(user).catalog(catalog).priority("HIGH").build();

        given(wishlistRepository.findById(1L)).willReturn(Optional.of(wishlist));
        given(catalogRepository.findById(10L)).willReturn(Optional.of(catalog));
        given(collectionRepository.save(any())).willThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> wishlistService.moveToCollection(1L, 1L,
            new MoveToCollectionRequest(null, null, null, null)))
            .isInstanceOf(RuntimeException.class);

        // save 실패 시 delete는 호출되지 않아야 함 (트랜잭션 코드 구조 검증)
        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    void getWishlists_size_100초과_예외() {
        assertThatThrownBy(() -> wishlistService.getWishlists(1L, null, PageRequest.of(0, 101)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getWishlists_priority_필터_적용() {
        given(wishlistRepository.findByUserIdAndPriority(1L, "HIGH", PageRequest.of(0, 20)))
            .willReturn(new PageImpl<>(List.of()));

        PageResponse<WishlistResponse> result = wishlistService.getWishlists(1L, "HIGH", PageRequest.of(0, 20));

        assertThat(result.data()).isEmpty();
        verify(wishlistRepository).findByUserIdAndPriority(1L, "HIGH", PageRequest.of(0, 20));
        verify(wishlistRepository, never()).findByUserId(any(), any());
    }
}
