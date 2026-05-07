package com.chanyong.gunpla.global.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final Cache<String, Bucket> rateLimitCache;

    @Around("@annotation(rateLimited)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = buildKey(rateLimited, joinPoint.getSignature().toShortString());
        int limit = rateLimited.limit();

        Bucket bucket = rateLimitCache.get(key, k -> Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)).build())
            .build());

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded: key={}", key);
            throw new RateLimitException(60L);
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimited rateLimited, String methodSignature) {
        if (rateLimited.byIp()) {
            HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
            return "ip:" + methodSignature + ":" + request.getRemoteAddr();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + methodSignature + ":" + principal.getId();
        }

        // 인증 정보 없으면 IP fallback
        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.currentRequestAttributes()).getRequest();
        return "ip:" + methodSignature + ":" + request.getRemoteAddr();
    }
}
