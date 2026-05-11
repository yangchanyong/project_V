package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 컬렉션 기본 CRUD 리포지토리.
 * Soft Delete 적용 엔티티로, 일반 조회는 @SQLRestriction에 의해 deleted_at IS NULL 조건이 자동 추가된다.
 * 배치 정리용 hard delete 메서드는 native query로 @SQLRestriction을 우회한다.
 */
public interface CollectionRepository extends JpaRepository<UserCollection, Long> {

    /**
     * Soft Delete된 레코드 중 threshold 이전에 삭제된 ID 목록을 조회한다.
     * S3 이미지 선삭제 후 hard delete에 사용된다.
     *
     * @param threshold 삭제 기준 시각 (이 시각보다 이전에 soft delete된 것만)
     * @return soft deleted 컬렉션 ID 목록
     */
    @Query(value = "SELECT id FROM user_collection WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    List<Long> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Soft Delete된 레코드를 DB에서 완전히 삭제한다 (Hard Delete).
     *
     * @param threshold 삭제 기준 시각
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query(value = "DELETE FROM user_collection WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
