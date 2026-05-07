package com.chanyong.gunpla.auth.controller;

import com.chanyong.gunpla.auth.dto.TokenResponse;
import com.chanyong.gunpla.auth.service.AuthService;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.ratelimit.RateLimited;
import com.chanyong.gunpla.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

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
