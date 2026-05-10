package com.chanyong.gunpla.global.config;

import com.chanyong.gunpla.global.auth.jwt.JwtAuthenticationFilter;
import com.chanyong.gunpla.global.auth.jwt.JwtProvider;
import com.chanyong.gunpla.global.auth.oauth2.CustomOAuth2UserService;
import com.chanyong.gunpla.global.auth.oauth2.CustomOidcUserService;
import com.chanyong.gunpla.global.auth.oauth2.OAuth2SuccessHandler;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService oAuth2UserService;
    private final CustomOidcUserService oidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${app.cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // OAuth2, Swagger, 카탈로그는 인증 없이 접근 가능
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                // 로그아웃/토큰 갱신은 Access Token 만료 시에도 허용 (Refresh Token 쿠키로 처리)
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)   // Kakao, Naver (non-OIDC)
                    .oidcUserService(oidcUserService)  // Google (OIDC)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((request, response, exception) -> {
                    log.error("OAuth2 login failed: {}", exception.getMessage(), exception);
                    response.sendError(401, "OAuth2 login failed: " + exception.getMessage());
                })
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // REST API — 미인증 요청 시 302 리다이렉트 대신 401 반환
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
            response.sendError(401, "Unauthorized");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
