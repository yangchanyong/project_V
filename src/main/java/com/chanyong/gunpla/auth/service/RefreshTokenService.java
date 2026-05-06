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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

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

    @Transactional
    public void revoke(String rawToken) {
        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAll(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        tokens.forEach(RefreshToken::revoke);
    }

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
