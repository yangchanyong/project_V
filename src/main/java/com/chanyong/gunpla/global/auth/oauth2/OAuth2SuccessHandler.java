package com.chanyong.gunpla.global.auth.oauth2;

import com.chanyong.gunpla.auth.service.RefreshTokenService;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.auth.jwt.JwtProperties;
import com.chanyong.gunpla.global.auth.jwt.JwtProvider;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-url:http://localhost:8080/swagger-ui/index.html}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 login"));

        // 기존 미만료 토큰 전체 revoke (중복 로그인 방지)
        refreshTokenService.revokeAll(user);

        String rawRefreshToken = refreshTokenService.issue(user);
        String accessToken = jwtProvider.generateAccessToken(principal);

        addRefreshTokenCookie(response, rawRefreshToken);

        log.info("OAuth2 login success: userId={}", principal.getId());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl + "?accessToken=" + accessToken);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        // Servlet Cookie API는 SameSite 미지원 → Set-Cookie 헤더 직접 설정
        response.addHeader("Set-Cookie",
            String.format("refreshToken=%s; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Lax; Max-Age=%d",
                rawToken, jwtProperties.getRefreshTokenExpirationMs() / 1000));
    }
}
