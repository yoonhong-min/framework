package com.example.framework.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtProps {
    /**
     * 예: hex:0123AB... (64 hex = 32 bytes) 또는 일반 문자열
     */
    private String secret;

    private String issuer = "framework";
    private long accessTtlSeconds = 1800;     // 30분
    private long refreshTtlSeconds = 1209600; // 14일
}
