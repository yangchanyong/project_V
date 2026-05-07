package com.chanyong.gunpla.user.repository;

import com.chanyong.gunpla.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // @SQLRestriction 우회 — soft deleted 레코드만 조회
    @Query(value = "SELECT id FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    List<Long> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query(value = "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
