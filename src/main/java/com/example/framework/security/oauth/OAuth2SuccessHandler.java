package com.example.framework.security.oauth;
import com.example.framework.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 1) 카카오 원본 데이터
        Map<String, Object> attrs = oAuth2User.getAttributes(); // kakao가 준 원본 JSON
        String kakaoId = String.valueOf(attrs.get("id"));
        String nickname = null;
        Object account = attrs.get("kakao_account");
        if (account instanceof Map<?, ?> acc) {
            Object profile = acc.get("profile");
            if (profile instanceof Map<?, ?> p) {
                Object nn = p.get("nickname");
                if (nn != null) nickname = String.valueOf(nn);
            }
        }

        // 2) 우리 JWT 발급 (테스트 편의로 닉네임도 claim에 포함)
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of("ROLE_USER"));
        claims.put("tokenVersion", 1);
        if (nickname != null) claims.put("nickname", nickname);

        String accessToken  = jwtService.createAccessToken(kakaoId, claims);
        String refreshToken = jwtService.createRefreshToken(kakaoId, claims);

        // (선택) refresh는 HttpOnly 쿠키로만 심어둠
        Cookie refresh = new Cookie("REFRESH_TOKEN", refreshToken);
        refresh.setHttpOnly(true);
        refresh.setPath("/");
        refresh.setMaxAge(60 * 60 * 24 * 14);
        response.addCookie(refresh);

        // 3) 로컬 디버그용 JSON 응답 (프론트 없이도 바로 확인 가능)
        response.setContentType("application/json;charset=UTF-8");
        String json = """
        {
          "accessToken": "%s",
          "kakaoId": "%s",
          "nickname": %s,
          "kakaoAttributes": %s
        }
        """.formatted(
                accessToken,
                kakaoId,
                nickname == null ? "null" : ("\"" + nickname.replace("\"","\\\"") + "\""),
                // attributes는 단순 문자열로 출력(진짜 JSON으로 만들고 싶으면 ObjectMapper 써도 됨)
                attrs.toString().replace("\"","\\\"")
        );
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
