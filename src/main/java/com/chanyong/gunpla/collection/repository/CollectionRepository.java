package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectionRepository extends JpaRepository<UserCollection, Long> {

    // @SQLRestriction 우회 — soft deleted 레코드만 조회
    @Query(value = "SELECT id FROM user_collection WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    List<Long> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query(value = "DELETE FROM user_collection WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
