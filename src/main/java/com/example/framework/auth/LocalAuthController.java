package com.example.framework.auth;

import com.example.framework.common.api.ApiResponse;
import com.example.framework.security.jwt.JwtService;
import com.example.framework.user.entity.User;
import com.example.framework.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
public class LocalAuthController {
    private final UserService userService;
    private final JwtService jwtService;

    record SignupReq(@Email String email, @NotBlank String password, @NotBlank String nickname) {}
    record LoginReq(@Email String email, @NotBlank String password) {}

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupReq req) {
        User u = userService.registerLocal(req.email(), req.password(), req.nickname());
        var claims = new HashMap<String,Object>();
        claims.put("roles", u.getRoles());
        claims.put("tokenVersion", u.getTokenVersion());
        claims.put("nickname", u.getNickname());
        String access = jwtService.createAccessToken(String.valueOf(u.getId()), claims);
        String refresh = jwtService.createRefreshToken(String.valueOf(u.getId()), claims);
        return ResponseEntity.ok(ApiResponse.ok(
                java.util.Map.of("accessToken", access, "refreshToken", refresh)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        User u = userService.loginLocal(req.email(), req.password());
        var claims = new HashMap<String,Object>();
        claims.put("roles", u.getRoles());
        claims.put("tokenVersion", u.getTokenVersion());
        claims.put("nickname", u.getNickname());
        String access = jwtService.createAccessToken(String.valueOf(u.getId()), claims);
        String refresh = jwtService.createRefreshToken(String.valueOf(u.getId()), claims);
        return ResponseEntity.ok(ApiResponse.ok(
                java.util.Map.of("accessToken", access, "refreshToken", refresh)));
    }
}