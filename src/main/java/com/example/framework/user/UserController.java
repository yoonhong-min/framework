package com.example.framework.me;

import com.example.framework.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error","no Authorization header"));
        }
        String token = auth.substring(7);
        Jws<Claims> jws = jwtService.parse(token);
        Claims c = jws.getPayload();

        return ResponseEntity.ok(Map.of(
                "userId", c.getSubject(),
                "nickname", c.get("nickname"),
                "roles", c.getOrDefault("roles", List.of())
        ));
    }
}
