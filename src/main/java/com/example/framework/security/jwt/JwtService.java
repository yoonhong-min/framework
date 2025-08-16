package com.example.framework.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProps props;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes;
        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is required");
        }
        if (secret.startsWith("hex:")) {
            keyBytes = HexFormat.of().parseHex(secret.substring(4));
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        // 256비트 이상 권장
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be >= 256 bits (32 bytes).");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);

        if (props.getAccessTtlSeconds() <= 0 || props.getRefreshTtlSeconds() <= 0) {
            throw new IllegalArgumentException("jwt.*-ttl-seconds must be > 0");
        }
    }

    public String createAccessToken(String userId, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId) // ★ 반드시 sub 설정
                .claims(claims)
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.getAccessTtlSeconds())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(String userId, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.getRefreshTtlSeconds())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public io.jsonwebtoken.Jws<Claims> parse(String token) {
        // issuer를 강제하고 싶으면 .requireIssuer(props.getIssuer()) 추가
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
