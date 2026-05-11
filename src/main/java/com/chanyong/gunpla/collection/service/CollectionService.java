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
import com.chanyong.gunpla.infrastructure.storage.StorageService;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 컬렉션 CRUD 및 빌드 상태 관리 서비스.
 * 모든 쓰기 작업은 {@link #findOwnedCollection}으로 소유권을 먼저 검증한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionQueryRepository collectionQueryRepository;
    private final CollectionImageRepository collectionImageRepository;
    private final CatalogRepository catalogRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * 내 컬렉션 목록을 필터 조건으로 페이징 조회한다.
     * 이미지는 MultipleBagFetchException 방지를 위해 별도 IN 쿼리로 조회한다.
     *
     * @param userId   로그인 유저 ID
     * @param req      buildStatus / grade 필터
     * @param pageable 페이지 정보 (size 최대 100)
     * @return 페이징된 컬렉션 응답 목록
     */
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
            CollectionResponse.from(c, imagesByCollectionId.getOrDefault(c.getId(), List.of()), storageService)
        );
        return PageResponse.of(responsePage);
    }

    /**
     * 새 컬렉션을 등록한다.
     *
     * @param userId 로그인 유저 ID
     * @param req    카탈로그 ID, 빌드 상태, 구매 정보
     * @return 생성된 컬렉션 ID
     * @throws com.chanyong.gunpla.global.exception.BusinessException CATALOG_NOT_FOUND(404)
     */
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

    /**
     * 컬렉션 단건 상세 조회.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @return 컬렉션 상세 (이미지 포함)
     */
    public CollectionResponse getCollection(Long userId, Long collectionId) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        List<CollectionImage> images = collectionImageRepository.findByCollection_IdIn(List.of(collectionId));
        return CollectionResponse.from(collection, images, storageService);
    }

    /**
     * 컬렉션 구매 정보·메모를 수정한다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @param req          수정할 구매 정보
     * @return 수정된 컬렉션 ID
     */
    @Transactional
    public CollectionCreateResponse updateCollection(Long userId, Long collectionId, CollectionUpdateRequest req) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        collection.update(req.purchasePrice(), req.purchaseCurrency(), req.purchaseDate(),
            req.purchasePlace(), req.memo());
        return new CollectionCreateResponse(collection.getId());
    }

    /**
     * 빌드 상태를 변경한다. 허용되지 않는 전이면 INVALID_STATUS_TRANSITION(400) 예외가 발생한다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @param next         변경할 빌드 상태
     * @return 변경된 컬렉션 ID + 새 빌드 상태
     */
    @Transactional
    public BuildStatusUpdateResponse changeStatus(Long userId, Long collectionId, BuildStatus next) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        collection.changeBuildStatus(next);
        return new BuildStatusUpdateResponse(collection.getId(), collection.getBuildStatus());
    }

    /**
     * 컬렉션을 소프트 삭제한다. 연결된 S3 이미지를 먼저 삭제 후 DB cascade 처리한다.
     * S3 삭제 실패는 경고 로그만 남기고 계속 진행한다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     */
    @Transactional
    public void deleteCollection(Long userId, Long collectionId) {
        UserCollection collection = findOwnedCollection(collectionId, userId);
        // S3 이미지 먼저 삭제 (DB cascade 전에 처리)
        List<CollectionImage> images = collectionImageRepository.findByCollection_IdIn(List.of(collectionId));
        images.forEach(img -> {
            try {
                storageService.delete(img.getS3Key());
            } catch (Exception e) {
                log.warn("S3 이미지 삭제 실패 (계속 진행): s3Key={}", img.getS3Key(), e);
            }
        });
        collectionRepository.delete(collection);
    }

    /**
     * 컬렉션을 조회하고 소유권을 검증한다.
     * CollectionImageService에서도 재사용한다.
     *
     * @param collectionId 컬렉션 PK
     * @param userId       소유자 유저 ID
     * @return 검증된 컬렉션 엔티티
     * @throws com.chanyong.gunpla.global.exception.BusinessException COLLECTION_NOT_FOUND(404), COLLECTION_ACCESS_DENIED(403)
     */
    UserCollection findOwnedCollection(Long collectionId, Long userId) {
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
