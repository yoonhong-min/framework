package com.example.framework.user;

import com.example.framework.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
            return ResponseEntity.status(401).body(Map.of("error", "no Authorization header"));
        }
        String token = auth.substring(7);

        // JJWT 0.12.x면 getPayload(), 0.11.x면 getBody() 사용
        Jws<Claims> jws = jwtService.parse(token);
        Claims c = jws.getPayload(); // ← 0.11.x는 getBody()

        String userId = c.getSubject();
        Object nickname = c.get("nickname");
        Object roles = c.getOrDefault("roles", List.of());

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "nickname", nickname,
                "roles", roles
        ));
    }
}