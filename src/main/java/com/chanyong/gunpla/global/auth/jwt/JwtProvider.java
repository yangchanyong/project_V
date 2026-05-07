package com.chanyong.gunpla.global.auth.jwt;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserPrincipal principal) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(principal.getId()))
            .issuedAt(new Date(now))
            .expiration(new Date(now + jwtProperties.getAccessTokenExpirationMs()))
            .signWith(getSigningKey())
            .compact();
    }

    // Refresh Token은 opaque UUID — 서버에서 SHA-256 해시로 저장하여 DB 대조
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        return Long.valueOf(parseAllClaims(token).getSubject());
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }
}
