package com.chanyong.gunpla.collection.service;

import com.chanyong.gunpla.collection.dto.ImageSaveRequest;
import com.chanyong.gunpla.collection.dto.ImageSaveResponse;
import com.chanyong.gunpla.collection.dto.PresignedUrlRequest;
import com.chanyong.gunpla.collection.dto.PresignedUrlResponse;
import com.chanyong.gunpla.collection.entity.CollectionImage;
import com.chanyong.gunpla.collection.entity.UserCollection;
import com.chanyong.gunpla.collection.repository.CollectionImageRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * 컬렉션 이미지 업로드·저장·삭제 서비스.
 * S3 Presigned URL 방식으로 클라이언트가 직접 S3에 업로드하고, 완료 후 메타데이터를 DB에 저장하는 흐름이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final int PRESIGNED_URL_EXPIRES_IN = 300;

    private final CollectionImageRepository collectionImageRepository;
    private final CollectionService collectionService;
    private final StorageService storageService;

    /**
     * S3 PUT용 Presigned URL을 발급한다.
     * s3Key는 서버에서 UUID를 포함해 생성하므로 경로 조작이 불가능하다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @param req          파일명, contentType, 파일 크기
     * @return Presigned URL, s3Key, 만료 시간(초)
     * @throws com.chanyong.gunpla.global.exception.BusinessException FILE_UPLOAD_VALIDATION_FAILED(400) — contentType 또는 크기 초과
     */
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(Long userId, Long collectionId, PresignedUrlRequest req) {
        validateFile(req.contentType(), req.fileSize());
        collectionService.findOwnedCollection(collectionId, userId);

        String s3Key = String.format("collections/%d/%s-%s", collectionId, UUID.randomUUID(), req.fileName());
        String presignedUrl = storageService.generatePutPresignedUrl(s3Key, req.contentType());

        return new PresignedUrlResponse(presignedUrl, s3Key, PRESIGNED_URL_EXPIRES_IN);
    }

    /**
     * S3 업로드 완료 후 이미지 메타데이터를 DB에 저장한다.
     * HeadObject로 S3 실재 여부를 검증한 뒤 저장한다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @param req          s3Key, 표시 순서
     * @return 생성된 이미지 ID
     * @throws com.chanyong.gunpla.global.exception.BusinessException FILE_UPLOAD_VALIDATION_FAILED(400) — S3에 파일 없음
     */
    @Transactional
    public ImageSaveResponse saveImage(Long userId, Long collectionId, ImageSaveRequest req) {
        UserCollection collection = collectionService.findOwnedCollection(collectionId, userId);

        if (!storageService.exists(req.s3Key())) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_VALIDATION_FAILED);
        }

        CollectionImage image = CollectionImage.builder()
            .collection(collection)
            .s3Key(req.s3Key())
            .displayOrder(req.displayOrder())
            .build();

        return new ImageSaveResponse(collectionImageRepository.save(image).getId());
    }

    /**
     * 이미지를 S3와 DB에서 함께 삭제한다.
     * 이미지가 해당 컬렉션 소속인지 검증한 뒤 삭제한다.
     *
     * @param userId       로그인 유저 ID
     * @param collectionId 컬렉션 PK
     * @param imageId      삭제할 이미지 PK
     * @throws com.chanyong.gunpla.global.exception.BusinessException COLLECTION_IMAGE_NOT_FOUND(404)
     */
    @Transactional
    public void deleteImage(Long userId, Long collectionId, Long imageId) {
        collectionService.findOwnedCollection(collectionId, userId);

        CollectionImage image = collectionImageRepository.findById(imageId)
            .filter(img -> img.getCollection().getId().equals(collectionId))
            .orElseThrow(() -> new BusinessException(ErrorCode.COLLECTION_IMAGE_NOT_FOUND));

        storageService.delete(image.getS3Key());
        collectionImageRepository.delete(image);
    }

    private void validateFile(String contentType, long fileSize) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType) || fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_VALIDATION_FAILED);
        }
    }
}
