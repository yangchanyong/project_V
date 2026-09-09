package com.chanyong.gunpla.auth.entity;

import com.chanyong.gunpla.global.entity.BaseTimeEntity;
import com.chanyong.gunpla.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
    }
)
/**
 * Refresh Token 엔티티.
 * 보안을 위해 토큰 원본은 저장하지 않으며 SHA-256 해시값만 DB에 보관한다.
 */
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토큰 소유자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** rawRefreshToken의 SHA-256 해시값 */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    /** 토큰 만료 일시 */
    // Why: MySQL 전용 DATETIME(6) 제거 — Hibernate 기본 timestamp 매핑 사용
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 명시적 무효화 여부 (로그아웃·토큰 로테이션 시 true) */
    @Column(nullable = false)
    private boolean revoked = false;

    @Builder
    public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * 토큰을 무효화한다.
     */
    public void revoke() {
        this.revoked = true;
    }
}
