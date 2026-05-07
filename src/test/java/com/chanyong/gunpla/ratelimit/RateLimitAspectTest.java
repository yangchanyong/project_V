package com.chanyong.gunpla.ratelimit;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.ratelimit.RateLimitAspect;
import com.chanyong.gunpla.global.ratelimit.RateLimitConfig;
import com.chanyong.gunpla.global.ratelimit.RateLimitException;
import com.chanyong.gunpla.global.ratelimit.RateLimited;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RateLimitAspectTest {

    private RateLimitAspect aspect;
    private Cache<String, Bucket> cache;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
        aspect = new RateLimitAspect(cache);

        // MockHttpServletRequest 설정 (RequestContextHolder 필요)
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void 한도_이내_요청은_정상_처리() throws Throwable {
        setAuthentication(1L);
        RateLimited rateLimited = mockAnnotation(5, false);
        ProceedingJoinPoint joinPoint = mockJoinPoint("TestController.test()");

        // 5번 모두 통과
        for (int i = 0; i < 5; i++) {
            aspect.around(joinPoint, rateLimited);
        }

        verify(joinPoint, times(5)).proceed();
    }

    @Test
    void 한도_초과시_RateLimitException_발생() throws Throwable {
        setAuthentication(1L);
        RateLimited rateLimited = mockAnnotation(3, false);
        ProceedingJoinPoint joinPoint = mockJoinPoint("TestController.limited()");

        // 3번 소진
        for (int i = 0; i < 3; i++) {
            aspect.around(joinPoint, rateLimited);
        }

        // 4번째는 429
        assertThatThrownBy(() -> aspect.around(joinPoint, rateLimited))
            .isInstanceOf(RateLimitException.class);
    }

    @Test
    void 유저별_버킷_격리() throws Throwable {
        RateLimited rateLimited = mockAnnotation(1, false);
        ProceedingJoinPoint joinPoint = mockJoinPoint("TestController.isolated()");

        // 유저 A: 1회 소진
        setAuthentication(1L);
        aspect.around(joinPoint, rateLimited);

        // 유저 B: 아직 소진 안 됨 → 통과
        setAuthentication(2L);
        aspect.around(joinPoint, rateLimited);

        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void byIp_true일때_IP_기반_키_사용() throws Throwable {
        RateLimited rateLimited = mockAnnotation(1, true);
        ProceedingJoinPoint joinPoint = mockJoinPoint("AuthController.refresh()");

        aspect.around(joinPoint, rateLimited);

        // 2번째는 429 (같은 IP)
        assertThatThrownBy(() -> aspect.around(joinPoint, rateLimited))
            .isInstanceOf(RateLimitException.class);
    }

    @Test
    void RateLimitException은_retryAfterSeconds_포함() throws Throwable {
        setAuthentication(1L);
        RateLimited rateLimited = mockAnnotation(1, false);
        ProceedingJoinPoint joinPoint = mockJoinPoint("TestController.check()");

        aspect.around(joinPoint, rateLimited); // 1회 소진

        try {
            aspect.around(joinPoint, rateLimited);
        } catch (RateLimitException e) {
            assertThat(e.getRetryAfterSeconds()).isEqualTo(60L);
        }
    }

    private void setAuthentication(Long userId) {
        // 최소한의 UserPrincipal (테스트용)
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(userId);
        when(principal.getUsername()).thenReturn(String.valueOf(userId));
        when(principal.getAuthorities()).thenReturn(java.util.List.of());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of())
        );
    }

    private RateLimited mockAnnotation(int limit, boolean byIp) {
        RateLimited rateLimited = mock(RateLimited.class);
        when(rateLimited.limit()).thenReturn(limit);
        when(rateLimited.byIp()).thenReturn(byIp);
        return rateLimited;
    }

    private ProceedingJoinPoint mockJoinPoint(String signatureStr) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn(signatureStr);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(null);
        return joinPoint;
    }
}
