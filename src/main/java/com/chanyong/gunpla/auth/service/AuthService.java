package com.chanyong.gunpla.auth.service;

import com.chanyong.gunpla.auth.dto.TokenResponse;
import com.chanyong.gunpla.auth.entity.RefreshToken;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    // 토큰 로테이션: 기존 토큰 revoke → 새 Access + Refresh Token 발급
    // 반환값: [0] AccessToken, [1] 새 rawRefreshToken (컨트롤러에서 쿠키에 설정)
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

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public record RefreshResult(TokenResponse tokenResponse, String newRawRefreshToken) {}
}
