package com.example.framework.security.jwt;

import com.example.framework.user.User;
import com.example.framework.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService; // ★ 추가
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        // 1) 토큰이 없으면 그냥 다음으로 통과 (회원가입/로그인/공개 API는 여기로 빠짐)
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            // 2) 토큰이 있을 때만 파싱/검증 수행
            Jws<Claims> jws = jwtService.parse(token);
            Claims claims = jws.getPayload();

            String userId = claims.getSubject();
            if (userId == null) {
                writeUnauthorized(response, "INVALID_TOKEN", "Missing subject");
                return;
            }

            // tokenVersion 비교
            User user = userService.findByIdOrThrow(Long.parseLong(userId));
            Number tokenVerNum = claims.get("tokenVersion", Number.class);
            int tokenVer = tokenVerNum == null ? -1 : tokenVerNum.intValue();
            if (tokenVer != user.getTokenVersion()) {
                writeUnauthorized(response, "TOKEN_VERSION_MISMATCH", "Token is no longer valid.");
                return;
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
            var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();

            var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authToken.setDetails(new HashMap<>(claims));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // 3) 정상이면 다음 필터/컨트롤러로
            chain.doFilter(request, response);

        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            // ★ JWT 관련 예외만 401
            writeUnauthorized(response, "UNAUTHORIZED", e.getClass().getSimpleName());
        }
        // ★ 여기서 전체 Exception을 다시 catch하지 않습니다.
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "code", code,
                "message", msg
        )));
    }
}
