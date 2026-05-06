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

    @Transactional
    public PresignedUrlResponse generatePresignedUrl(Long userId, Long collectionId, PresignedUrlRequest req) {
        validateFile(req.contentType(), req.fileSize());
        collectionService.findOwnedCollection(collectionId, userId);

        String s3Key = String.format("collections/%d/%s-%s", collectionId, UUID.randomUUID(), req.fileName());
        String presignedUrl = storageService.generatePutPresignedUrl(s3Key, req.contentType());

        return new PresignedUrlResponse(presignedUrl, s3Key, PRESIGNED_URL_EXPIRES_IN);
    }

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
