package com.chanyong.gunpla.auth.controller;

import com.chanyong.gunpla.auth.dto.TokenResponse;
import com.chanyong.gunpla.auth.service.AuthService;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.ratelimit.RateLimited;
import com.chanyong.gunpla.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 토큰 갱신 및 로그아웃 API.
 * Refresh Token은 HttpOnly 쿠키로 전달받으며, 인증 없이 접근 가능하다.
 */
@Tag(name = "Auth", description = "토큰 갱신 및 로그아웃 (Refresh Token 쿠키 사용, 인증 불필요)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    /**
     * Refresh Token으로 Access Token과 Refresh Token을 재발급한다 (토큰 로테이션).
     * IP당 분당 10건으로 Rate Limit이 걸려있다.
     *
     * @param rawRefreshToken HttpOnly 쿠키의 Refresh Token
     * @param response        새 Refresh Token 쿠키 설정용
     * @return 새 Access Token과 만료 시간
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token 쿠키로 새 Access Token과 Refresh Token을 발급한다 (토큰 로테이션). IP당 분당 10건 제한.")
    @RateLimited(limit = 10, byIp = true)
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
        @CookieValue(name = "refreshToken", required = false) String rawRefreshToken,
        HttpServletResponse response
    ) {
        if (rawRefreshToken == null) {
            throw new com.chanyong.gunpla.global.exception.BusinessException(
                com.chanyong.gunpla.global.exception.ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AuthService.RefreshResult result = authService.refresh(rawRefreshToken);
        setRefreshTokenCookie(response, result.newRawRefreshToken());
        return ApiResponse.of(result.tokenResponse());
    }

    /**
     * 로그아웃 처리. Refresh Token을 revoke하고 쿠키를 만료시킨다.
     * 쿠키가 없어도 정상 처리된다 (이미 만료된 상태로 간주).
     *
     * @param rawRefreshToken HttpOnly 쿠키의 Refresh Token (없을 수도 있음)
     * @param response        쿠키 삭제용
     */
    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하고 쿠키를 삭제한다. 쿠키 없이도 정상 처리된다.")
    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = "refreshToken", required = false) String rawRefreshToken,
        HttpServletResponse response
    ) {
        if (rawRefreshToken != null) {
            authService.logout(rawRefreshToken);
        }
        clearRefreshTokenCookie(response);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        response.addHeader("Set-Cookie",
            String.format("refreshToken=%s; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Lax; Max-Age=%d",
                rawToken, jwtProperties.getRefreshTokenExpirationMs() / 1000));
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
            "refreshToken=; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Lax; Max-Age=0");
    }
}
