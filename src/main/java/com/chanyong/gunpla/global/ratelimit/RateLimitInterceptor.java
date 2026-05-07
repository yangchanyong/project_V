package com.chanyong.gunpla.global.ratelimit;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.ErrorResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, Bucket> rateLimitCache;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return true; // 익명 요청은 skip (catalog 등 공개 API)
        }

        String key = "general:user:" + principal.getId();
        Bucket bucket = rateLimitCache.get(key, k -> Bucket.builder()
            .addLimit(RateLimitConfig.generalConfig().getBandwidths()[0])
            .build());

        if (!bucket.tryConsume(1)) {
            log.warn("General rate limit exceeded: userId={}", principal.getId());
            ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Retry-After", "60");
            objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
            return false;
        }

        return true;
    }
}
