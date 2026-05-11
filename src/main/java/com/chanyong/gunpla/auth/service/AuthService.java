package com.chanyong.gunpla.auth.service;

import com.chanyong.gunpla.auth.dto.TokenResponse;
import com.chanyong.gunpla.auth.entity.RefreshToken;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 갱신 및 로그아웃 서비스.
 * 토큰 로테이션 방식을 사용한다 — refresh 호출마다 기존 토큰을 revoke하고 새 토큰을 발급한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    /**
     * Refresh Token을 검증하고 새 Access Token + Refresh Token을 발급한다.
     * 기존 Refresh Token은 즉시 revoke된다 (토큰 로테이션).
     *
     * @param rawRefreshToken 클라이언트 쿠키의 Refresh Token 원본값
     * @return 새 Access Token 응답과 새 rawRefreshToken
     * @throws com.chanyong.gunpla.global.exception.BusinessException INVALID_REFRESH_TOKEN(401)
     */
    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenService.validate(rawRefreshToken);
        token.revoke();

        String newRawRefreshToken = refreshTokenService.issue(token.getUser());
        String accessToken = jwtProvider.generateAccessToken(UserPrincipal.of(token.getUser()));

        return new RefreshResult(
            new TokenResponse(accessToken, jwtProperties.getAccessTokenExpirationMs()),
            newRawRefreshToken
        );
    }

    /**
     * Refresh Token을 revoke하여 로그아웃 처리한다.
     *
     * @param rawRefreshToken 클라이언트 쿠키의 Refresh Token 원본값
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public record RefreshResult(TokenResponse tokenResponse, String newRawRefreshToken) {}
}
