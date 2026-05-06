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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionImageServiceTest {

    @InjectMocks
    private CollectionImageService collectionImageService;

    @Mock
    private CollectionImageRepository collectionImageRepository;
    @Mock
    private CollectionService collectionService;
    @Mock
    private StorageService storageService;

    // ──────────────────────────────────────────
    // generatePresignedUrl
    // ──────────────────────────────────────────

    @Test
    void generatePresignedUrl_허용되지않는_contentType_예외() {
        PresignedUrlRequest req = new PresignedUrlRequest("photo.gif", "image/gif", 1024L);

        assertThatThrownBy(() -> collectionImageService.generatePresignedUrl(1L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.FILE_UPLOAD_VALIDATION_FAILED);
    }

    @Test
    void generatePresignedUrl_파일크기초과_예외() {
        long overSize = 10 * 1024 * 1024L + 1;
        PresignedUrlRequest req = new PresignedUrlRequest("photo.jpg", "image/jpeg", overSize);

        assertThatThrownBy(() -> collectionImageService.generatePresignedUrl(1L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.FILE_UPLOAD_VALIDATION_FAILED);
    }

    @Test
    void generatePresignedUrl_컬렉션없음_예외() {
        PresignedUrlRequest req = new PresignedUrlRequest("photo.jpg", "image/jpeg", 1024L);
        given(collectionService.findOwnedCollection(1L, 1L))
            .willThrow(new BusinessException(ErrorCode.COLLECTION_NOT_FOUND));

        assertThatThrownBy(() -> collectionImageService.generatePresignedUrl(1L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_NOT_FOUND);
    }

    @Test
    void generatePresignedUrl_소유권없음_예외() {
        PresignedUrlRequest req = new PresignedUrlRequest("photo.jpg", "image/jpeg", 1024L);
        given(collectionService.findOwnedCollection(1L, 99L))
            .willThrow(new BusinessException(ErrorCode.COLLECTION_ACCESS_DENIED));

        assertThatThrownBy(() -> collectionImageService.generatePresignedUrl(99L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_ACCESS_DENIED);
    }

    @Test
    void generatePresignedUrl_성공() {
        PresignedUrlRequest req = new PresignedUrlRequest("photo.jpg", "image/jpeg", 1024L);
        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(mock(UserCollection.class));
        given(storageService.generatePutPresignedUrl(anyString(), anyString()))
            .willReturn("https://s3.example.com/presigned");

        PresignedUrlResponse response = collectionImageService.generatePresignedUrl(1L, 1L, req);

        assertThat(response.presignedUrl()).isEqualTo("https://s3.example.com/presigned");
        assertThat(response.s3Key()).contains("collections/1/");
        assertThat(response.expiresIn()).isEqualTo(300);
    }

    // ──────────────────────────────────────────
    // saveImage
    // ──────────────────────────────────────────

    @Test
    void saveImage_S3에파일없음_예외() {
        ImageSaveRequest req = new ImageSaveRequest("collections/1/uuid-photo.jpg", 0);
        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(mock(UserCollection.class));
        given(storageService.exists(req.s3Key())).willReturn(false);

        assertThatThrownBy(() -> collectionImageService.saveImage(1L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.FILE_UPLOAD_VALIDATION_FAILED);
    }

    @Test
    void saveImage_소유권없음_예외() {
        ImageSaveRequest req = new ImageSaveRequest("collections/1/uuid-photo.jpg", 0);
        given(collectionService.findOwnedCollection(1L, 99L))
            .willThrow(new BusinessException(ErrorCode.COLLECTION_ACCESS_DENIED));

        assertThatThrownBy(() -> collectionImageService.saveImage(99L, 1L, req))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_ACCESS_DENIED);
    }

    @Test
    void saveImage_성공() {
        ImageSaveRequest req = new ImageSaveRequest("collections/1/uuid-photo.jpg", 0);
        UserCollection collection = mock(UserCollection.class);
        CollectionImage savedImage = mock(CollectionImage.class);

        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(collection);
        given(storageService.exists(req.s3Key())).willReturn(true);
        given(collectionImageRepository.save(any())).willReturn(savedImage);
        given(savedImage.getId()).willReturn(10L);

        ImageSaveResponse response = collectionImageService.saveImage(1L, 1L, req);

        assertThat(response.id()).isEqualTo(10L);
        verify(collectionImageRepository).save(any(CollectionImage.class));
    }

    // ──────────────────────────────────────────
    // deleteImage
    // ──────────────────────────────────────────

    @Test
    void deleteImage_이미지없음_예외() {
        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(mock(UserCollection.class));
        given(collectionImageRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collectionImageService.deleteImage(1L, 1L, 99L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_IMAGE_NOT_FOUND);
    }

    @Test
    void deleteImage_다른컬렉션_이미지_예외() {
        CollectionImage image = mock(CollectionImage.class);
        UserCollection otherCollection = mock(UserCollection.class);
        given(otherCollection.getId()).willReturn(999L);
        given(image.getCollection()).willReturn(otherCollection);

        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(mock(UserCollection.class));
        given(collectionImageRepository.findById(1L)).willReturn(Optional.of(image));

        assertThatThrownBy(() -> collectionImageService.deleteImage(1L, 1L, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.COLLECTION_IMAGE_NOT_FOUND);
    }

    @Test
    void deleteImage_성공() {
        CollectionImage image = mock(CollectionImage.class);
        UserCollection collection = mock(UserCollection.class);
        given(collection.getId()).willReturn(1L);
        given(image.getCollection()).willReturn(collection);
        given(image.getS3Key()).willReturn("collections/1/uuid-photo.jpg");

        given(collectionService.findOwnedCollection(1L, 1L)).willReturn(mock(UserCollection.class));
        given(collectionImageRepository.findById(1L)).willReturn(Optional.of(image));

        collectionImageService.deleteImage(1L, 1L, 1L);

        verify(storageService).delete("collections/1/uuid-photo.jpg");
        verify(collectionImageRepository).delete(image);
    }
}
