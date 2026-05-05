package com.chanyong.gunpla.collection.service;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.catalog.repository.CatalogRepository;
import com.chanyong.gunpla.collection.dto.*;
import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.UserCollection;
import com.chanyong.gunpla.collection.repository.CollectionImageRepository;
import com.chanyong.gunpla.collection.repository.CollectionQueryRepository;
import com.chanyong.gunpla.collection.repository.CollectionRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.PageResponse;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @InjectMocks
    private CollectionService collectionService;

    @Mock private CollectionRepository collectionRepository;
    @Mock private CollectionQueryRepository collectionQueryRepository;
    @Mock private CollectionImageRepository collectionImageRepository;
    @Mock private CatalogRepository catalogRepository;
    @Mock private UserRepository userRepository;

    private UserCollection collectionWithUser(User user, BuildStatus status) {
        return UserCollection.builder()
            .user(user)
            .catalog(mock(GunplaCatalog.class))
            .buildStatus(status)
            .build();
    }

    @Test
    void createCollection_정상_생성() {
        User user = mock(User.class);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        UserCollection saved = mock(UserCollection.class);
        CollectionCreateRequest req = new CollectionCreateRequest(10L, BuildStatus.UNBUILT, null, null, null, null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(catalogRepository.findById(10L)).willReturn(Optional.of(catalog));
        given(collectionRepository.save(any())).willReturn(saved);
        given(saved.getId()).willReturn(1L);

        CollectionCreateResponse result = collectionService.createCollection(1L, req);

        assertThat(result.id()).isEqualTo(1L);
        verify(collectionRepository).save(any());
    }

    @Test
    void createCollection_catalog없음_예외() {
        User user = mock(User.class);
        CollectionCreateRequest req = new CollectionCreateRequest(999L, BuildStatus.UNBUILT, null, null, null, null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(catalogRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.createCollection(1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CATALOG_NOT_FOUND);
    }

    @Test
    void changeStatus_정상_전이_UNBUILT_to_IN_PROGRESS() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        UserCollection collection = collectionWithUser(user, BuildStatus.UNBUILT);
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));

        BuildStatusUpdateResponse result = collectionService.changeStatus(1L, 1L, BuildStatus.IN_PROGRESS);

        assertThat(result.buildStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    }

    @Test
    void changeStatus_역방향복구_IN_PROGRESS_to_UNBUILT() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        UserCollection collection = collectionWithUser(user, BuildStatus.IN_PROGRESS);
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));

        BuildStatusUpdateResponse result = collectionService.changeStatus(1L, 1L, BuildStatus.UNBUILT);

        assertThat(result.buildStatus()).isEqualTo(BuildStatus.UNBUILT);
    }

    @Test
    void changeStatus_단계건너뜀_예외() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        UserCollection collection = collectionWithUser(user, BuildStatus.UNBUILT);
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));

        assertThatThrownBy(() -> collectionService.changeStatus(1L, 1L, BuildStatus.COMPLETED))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void deleteCollection_소유권검증_통과() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        UserCollection collection = collectionWithUser(user, BuildStatus.UNBUILT);
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));

        collectionService.deleteCollection(1L, 1L);

        verify(collectionRepository).delete(collection);
    }

    @Test
    void deleteCollection_다른유저_403예외() {
        User otherUser = mock(User.class);
        given(otherUser.getId()).willReturn(99L);
        UserCollection collection = collectionWithUser(otherUser, BuildStatus.UNBUILT);
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));

        assertThatThrownBy(() -> collectionService.deleteCollection(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_ACCESS_DENIED);
    }

    @Test
    void getCollections_size_100초과_예외() {
        assertThatThrownBy(() -> collectionService.getCollections(
            1L, new CollectionSearchRequest(null, null), PageRequest.of(0, 101)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getCollections_빈결과() {
        given(collectionQueryRepository.searchByUser(1L, null, null, PageRequest.of(0, 20)))
            .willReturn(new PageImpl<>(List.of()));

        PageResponse<CollectionResponse> result = collectionService.getCollections(
            1L, new CollectionSearchRequest(null, null), PageRequest.of(0, 20));

        assertThat(result.data()).isEmpty();
    }
}
