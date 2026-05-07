package com.chanyong.gunpla.global.batch;

import com.chanyong.gunpla.collection.entity.CollectionImage;
import com.chanyong.gunpla.collection.repository.CollectionImageRepository;
import com.chanyong.gunpla.collection.repository.CollectionRepository;
import com.chanyong.gunpla.infrastructure.storage.StorageService;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoftDeleteCleanupScheduler {

    private final UserRepository userRepository;
    private final CollectionRepository collectionRepository;
    private final CollectionImageRepository collectionImageRepository;
    private final StorageService storageService;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupSoftDeleted() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        log.info("Soft delete cleanup started: threshold={}", threshold);

        // 1. soft-deleted user_collection의 S3 이미지 선제 삭제
        List<Long> collectionIds = collectionRepository.findIdsByDeletedAtBefore(threshold);
        if (!collectionIds.isEmpty()) {
            List<CollectionImage> images = collectionImageRepository.findByCollection_IdIn(collectionIds);
            images.forEach(img -> {
                try {
                    storageService.delete(img.getS3Key());
                } catch (Exception e) {
                    log.warn("S3 image delete failed: s3Key={}", img.getS3Key(), e);
                }
            });

            int deletedCollections = collectionRepository.hardDeleteByDeletedAtBefore(threshold);
            log.info("Hard deleted user_collection: count={}", deletedCollections);
        }

        // 2. soft-deleted users hard delete (cascade로 하위 레코드 삭제)
        int deletedUsers = userRepository.hardDeleteByDeletedAtBefore(threshold);
        log.info("Hard deleted users: count={}", deletedUsers);

        log.info("Soft delete cleanup finished");
    }
}
