package com.chanyong.gunpla.collection.service;

import com.chanyong.gunpla.catalog.repository.CatalogRepository;
import com.chanyong.gunpla.collection.dto.*;
import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.CollectionImage;
import com.chanyong.gunpla.collection.entity.UserCollection;
import com.chanyong.gunpla.collection.repository.CollectionImageRepository;
import com.chanyong.gunpla.collection.repository.CollectionQueryRepository;
import com.chanyong.gunpla.collection.repository.CollectionRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.PageResponse;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionQueryRepository collectionQueryRepository;
    private final CollectionImageRepository collectionImageRepository;
    private final CatalogRepository catalogRepository;
    private final UserRepository userRepository;

    public PageResponse<CollectionResponse> getCollections(Long userId, CollectionSearchRequest req, Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Page<UserCollection> page = collectionQueryRepository.searchByUser(
            userId, req.buildStatus(), req.grade(), pageable
        );

        List<Long> ids = page.getContent().stream().map(UserCollection::getId).toList();
        Map<Long, List<CollectionImage>> imagesByCollectionId = fetchImagesByIds(ids);

        Page<CollectionResponse> responsePage = page.map(c ->
            CollectionResponse.from(c, imagesByCollectionId.getOrDefault(c.getId(), List.of()))
        );
        return PageResponse.of(responsePage);
    }

    @Transactional
    public CollectionCreateResponse createCollection(Long userId, CollectionCreateRequest req) {
        User user = findUser(userId);
        var catalog = catalogRepository.findById(req.catalogId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CATALOG_NOT_FOUND));

        UserCollection collection = UserCollection.builder()
            .user(user)
            .catalog(catalog)
            .buildStatus(req.buildStatus())
            .purchasePrice(req.purchasePrice())
            .purchaseCurrency(req.purchaseCurrency())
            .purchaseDate(req.purchaseDate())
            .purchasePlace(req.purchasePlace())
            .memo(req.memo())
            .build();

        return new CollectionCreateResponse(collectionRepository.save(collection).getId());
    }

    public CollectionResponse getCollection(Long userId, Long collectionId) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        List<CollectionImage> images = collectionImageRepository.findByCollection_IdIn(List.of(collectionId));
        return CollectionResponse.from(collection, images);
    }

    @Transactional
    public CollectionCreateResponse updateCollection(Long userId, Long collectionId, CollectionUpdateRequest req) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        collection.update(req.purchasePrice(), req.purchaseCurrency(), req.purchaseDate(),
            req.purchasePlace(), req.memo());
        return new CollectionCreateResponse(collection.getId());
    }

    @Transactional
    public BuildStatusUpdateResponse changeStatus(Long userId, Long collectionId, BuildStatus next) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        collection.changeBuildStatus(next);
        return new BuildStatusUpdateResponse(collection.getId(), collection.getBuildStatus());
    }

    @Transactional
    public void deleteCollection(Long userId, Long collectionId) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        collectionRepository.delete(collection);
    }

    private UserCollection findOwnedCollection(Long collectionId, Long userId) {
        UserCollection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COLLECTION_NOT_FOUND));
        if (!collection.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.COLLECTION_ACCESS_DENIED);
        }
        return collection;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Map<Long, List<CollectionImage>> fetchImagesByIds(List<Long> collectionIds) {
        if (collectionIds.isEmpty()) return Map.of();
        return collectionImageRepository.findByCollection_IdIn(collectionIds).stream()
            .collect(Collectors.groupingBy(img -> img.getCollection().getId()));
    }
}
