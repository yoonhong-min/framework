package com.example.framework.auth;

import com.example.framework.common.api.ApiResponse;
import com.example.framework.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @Value("${jwt.refresh-token.cookie-name:REFRESH_TOKEN}")
    private String refreshCookieName;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Optional<String> refreshOpt = readCookie(request, refreshCookieName);
        if (refreshOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Refresh token missing"));
        }
        try {
            Jws<Claims> jws = jwtService.parse(refreshOpt.get());
            String userId = jws.getPayload().getSubject();
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", jws.getPayload().get("roles"));
            claims.put("tokenVersion", jws.getPayload().get("tokenVersion"));

            String newAccess = jwtService.createAccessToken(userId, claims);
            String newRefresh = jwtService.createRefreshToken(userId, claims); // 회전: 새로 발급

            // 실제 운영: 이전 refresh 토큰은 서버 저장소에서 "폐기" 처리 (토큰ID/버전 관리)

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "accessToken", newAccess,
                    "refreshToken", newRefresh // 필요 시 본문이 아닌 쿠키로만 전달 가능
            )));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Invalid refresh token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("principal", principal)));
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return Optional.ofNullable(c.getValue());
        }
        return Optional.empty();
    }
}