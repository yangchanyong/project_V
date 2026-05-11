package com.chanyong.gunpla.auth.service;

import com.chanyong.gunpla.auth.entity.RefreshToken;
import com.chanyong.gunpla.auth.repository.RefreshTokenRepository;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.auth.jwt.JwtProvider;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Refresh Token 발급·검증·무효화 서비스.
 * 토큰은 DB에 SHA-256 해시로만 저장된다. 원본(raw) 값은 클라이언트 쿠키에만 존재한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    /**
     * 새 Refresh Token을 발급하고 DB에 해시를 저장한다.
     *
     * @param user 토큰을 발급할 유저
     * @return 클라이언트에 전달할 rawRefreshToken (DB에는 해시만 저장됨)
     */
    @Transactional
    public String issue(User user) {
        String rawToken = jwtProvider.generateRefreshToken();
        String hash = sha256(rawToken);

        RefreshToken token = RefreshToken.builder()
            .user(user)
            .tokenHash(hash)
            .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
            .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    /**
     * Refresh Token의 유효성을 검증한다.
     * revoked 상태이거나 만료된 경우 INVALID_REFRESH_TOKEN(401) 예외를 던진다.
     *
     * @param rawToken 클라이언트 쿠키의 rawRefreshToken
     * @return 유효한 RefreshToken 엔티티
     * @throws com.chanyong.gunpla.global.exception.BusinessException INVALID_REFRESH_TOKEN(401)
     */
    @Transactional
    public RefreshToken validate(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return token;
    }

    /**
     * 특정 Refresh Token을 무효화한다.
     * 해당 토큰이 없으면 아무 작업도 하지 않는다.
     *
     * @param rawToken 무효화할 rawRefreshToken
     */
    @Transactional
    public void revoke(String rawToken) {
        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    /**
     * 유저의 모든 활성 Refresh Token을 무효화한다.
     * 소셜 로그인 성공 시 중복 로그인 방지를 위해 호출된다.
     *
     * @param user 무효화할 유저
     */
    @Transactional
    public void revokeAll(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        tokens.forEach(RefreshToken::revoke);
    }

    /**
     * 만료된 Refresh Token을 DB에서 정리한다. 매일 03:00에 실행된다.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired refresh tokens cleaned up");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
