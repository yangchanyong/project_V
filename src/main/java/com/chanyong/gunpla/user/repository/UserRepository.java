package com.chanyong.gunpla.user.repository;

import com.chanyong.gunpla.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 유저 리포지토리.
 * 계정 조회는 반드시 (provider, providerId) 조합을 사용해야 한다. email로 조회하지 않는다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 소셜 로그인 제공자와 제공자 유저 ID로 유저를 조회한다.
     * OAuth2 로그인 시 기존 계정 여부를 확인하는 데 사용한다.
     *
     * @param provider   소셜 로그인 제공자 (google / kakao / naver)
     * @param providerId 소셜 제공자의 유저 고유 ID
     * @return 해당 계정의 유저 (없으면 빈 Optional)
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Soft Delete된 유저 중 threshold 이전에 삭제된 ID 목록을 조회한다.
     *
     * @param threshold 삭제 기준 시각
     * @return soft deleted 유저 ID 목록
     */
    @Query(value = "SELECT id FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    List<Long> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Soft Delete된 유저를 DB에서 완전히 삭제한다 (Hard Delete).
     *
     * @param threshold 삭제 기준 시각
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query(value = "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    int hardDeleteByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
