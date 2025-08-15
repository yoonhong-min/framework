package com.example.framework.user;

import com.example.framework.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.Jar;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error","no token"));
        }
        var jws = jwtService.parse(auth.substring(7)); // 0.12.x면 getPayload(), 0.11.x면 getBody()
        var claims = jws.getPayload();                 // 또는 getBody()
        String userId = claims.getSubject();
        String nickname = (String) claims.get("nickname");
        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) claims.getOrDefault("roles", java.util.List.of());

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "nickname", nickname,
                "roles", roles
        ));
    }
}