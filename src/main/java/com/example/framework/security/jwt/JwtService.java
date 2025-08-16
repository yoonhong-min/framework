package com.example.framework.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.refresh-token.expiry-days:14}")
    private long refreshExpiryDays;

    @Value("${jwt.issuer:framework}")
    private String issuer;

    @Value("${jwt.access-ttl-seconds:1800}")   // 기본 30분
    private long accessTtlSeconds;

    @Value("${jwt.refresh-ttl-seconds:1209600}") // 기본 14일
    private long refreshTtlSeconds;

    private SecretKey key;

    @PostConstruct
    void init() {
        // 최소 256bit(HMAC-SHA) 보장: 운영은 충분히 긴 랜덤 문자열/키 사용
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)                 // ★ 필수
                .claims(claims)
                .issuer(issuer)                  // 선택(파서에서 requireIssuer 쓰면 필수)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(String userId, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpiryDays * 24 * 60 * 60);
        return Jwts.builder()
                .subject(userId)                 // ★ 필수
                .claims(claims)
                .issuer(issuer)                  // 선택(파서에서 requireIssuer 쓰면 필수)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }
}