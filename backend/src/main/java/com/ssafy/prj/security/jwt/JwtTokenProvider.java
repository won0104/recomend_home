package com.ssafy.prj.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Access/Refresh JWT를 발급·검증하는 단일 진입점이다. */
@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessSeconds;
    private final long refreshSeconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-seconds}") long accessSeconds,
            @Value("${jwt.refresh-expiration-seconds}") long refreshSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }
    public String createAccessToken(String userId, String role) { return create(userId, role, "access", accessSeconds); }
    public String createRefreshToken(String userId, String role) { return create(userId, role, "refresh", refreshSeconds); }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public long refreshSeconds() { return refreshSeconds; }
    private String create(String userId, String role, String type, long seconds) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId).claim("role", role).claim("type", type)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(seconds))).signWith(key).compact();
    }
}
