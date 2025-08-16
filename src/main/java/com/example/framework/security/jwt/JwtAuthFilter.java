package com.example.framework.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                var jws = jwtService.parse(token);
                var claims = jws.getPayload();

                String userId = claims.getSubject(); // sub
                if (userId != null) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
                    var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();

                    var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    // details에 클레임 전체를 실어 컨트롤러에서 참고 가능
                    authToken.setDetails(new HashMap<>(claims));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            chain.doFilter(request, response);
        } catch (Exception ex) {
            // 401 JSON 표준 응답
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            var body = Map.of(
                    "code", "UNAUTHORIZED",
                    "message", "Invalid or expired token",
                    "detail", ex.getClass().getSimpleName()
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
        }
    }
}
