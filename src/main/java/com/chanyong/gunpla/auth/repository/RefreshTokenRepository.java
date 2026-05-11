package com.chanyong.gunpla.auth.repository;

import com.chanyong.gunpla.auth.entity.RefreshToken;
import com.chanyong.gunpla.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token 리포지토리.
 * 토큰 조회는 항상 SHA-256 해시값으로 수행한다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 해시값으로 Refresh Token을 조회한다.
     *
     * @param tokenHash rawRefreshToken의 SHA-256 해시값
     * @return 해당 토큰 (없으면 빈 Optional)
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 유저의 활성 상태(revoked=false)인 Refresh Token 목록을 조회한다.
     * 중복 로그인 방지를 위해 revokeAll 시 사용한다.
     *
     * @param user 조회할 유저
     * @return 활성 Refresh Token 목록
     */
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    /**
     * 만료된 Refresh Token을 일괄 삭제한다.
     *
     * @param cutoff 이 시각 이전에 만료된 토큰을 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
