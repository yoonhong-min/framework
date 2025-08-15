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

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiry-minutes:30}")
    private long accessExpiryMinutes;

    @Value("${jwt.refresh-token.expiry-days:14}")
    private long refreshExpiryDays;

    private SecretKey key;

    @PostConstruct
    void init() {
        // 최소 256bit(HMAC-SHA) 보장: 운영은 충분히 긴 랜덤 문자열/키 사용
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpiryMinutes * 60);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpiryDays * 24 * 60 * 60);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
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