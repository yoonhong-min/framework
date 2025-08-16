package com.example.framework.security.oauth;

import com.example.framework.security.jwt.JwtService;
import com.example.framework.user.User;
import com.example.framework.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService; // ← 소셜 계정 upsert해서 우리 User로 묶는 서비스

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 어떤 provider로 들어왔는지 (kakao | naver)
        String registrationId = (authentication instanceof OAuth2AuthenticationToken t)
                ? t.getAuthorizedClientRegistrationId()
                : "unknown";

        Map<String, Object> attrs = oAuth2User.getAttributes();

        // ── provider별 프로필 추출 ─────────────────────────────────────────────
        String externalId = null;     // 카카오/네이버의 고유 id (providerUserId)
        String displayName = null;    // 닉네임/이름
        String emailFromProvider = null;

        if ("kakao".equals(registrationId)) {
            externalId = String.valueOf(attrs.get("id"));
            Object acc = attrs.get("kakao_account");
            if (acc instanceof Map<?, ?> a) {
                Object prof = a.get("profile");
                if (prof instanceof Map<?, ?> p && p.get("nickname") != null) {
                    displayName = String.valueOf(p.get("nickname"));
                }
                if (a.get("email") != null) {
                    emailFromProvider = String.valueOf(a.get("email"));
                }
            }
        } else if ("naver".equals(registrationId)) {
            Object resp = attrs.get("response");
            if (resp instanceof Map<?, ?> r) {
                externalId = String.valueOf(r.get("id"));
                Object nick = (r.get("nickname") != null) ? r.get("nickname") : r.get("name");
                if (nick != null) displayName = String.valueOf(nick);
                if (r.get("email") != null) emailFromProvider = String.valueOf(r.get("email"));
            }
        }

        if (externalId == null) {
            response.sendError(400, "OAuth2 profile missing id");
            return;
        }

        // ── 우리 쪽 User로 upsert(연결/가입) ──────────────────────────────────
        User user = userService.upsertOAuthUser(
                registrationId,         // kakao | naver
                externalId,             // provider user id
                emailFromProvider,      // null 가능
                displayName             // 닉네임
        );

        // ── JWT 발급 (우리 API 인증용) ───────────────────────────────────────
        var claims = new HashMap<String, Object>();
        claims.put("roles", user.getRoles());
        claims.put("tokenVersion", user.getTokenVersion());
        claims.put("nickname", user.getNickname());

        String accessToken  = jwtService.createAccessToken(String.valueOf(user.getId()), claims);
        String refreshToken = jwtService.createRefreshToken(String.valueOf(user.getId()), claims);

        // refresh는 HttpOnly 쿠키로
        Cookie refresh = new Cookie("REFRESH_TOKEN", refreshToken);
        refresh.setHttpOnly(true);
        refresh.setPath("/");
        refresh.setMaxAge(60 * 60 * 24 * 14);
        response.addCookie(refresh);

        // ── 개발용: JSON으로 바로 확인 (프론트 없이도 테스트 가능) ──────────────
        response.setContentType("application/json;charset=UTF-8");
        String json = """
        {"provider":"%s","userId":%s,"displayName":%s,"accessToken":"%s"}
        """.formatted(
                registrationId,
                user.getId(),
                user.getNickname() == null ? "null" : "\"" + user.getNickname().replace("\"","\\\"") + "\"",
                accessToken
        );
        response.getWriter().write(json);
        response.getWriter().flush();

        // ※ 프론트 앱으로 보내고 싶으면 위 JSON 대신 아래 한 줄 사용:
        // response.sendRedirect("http://localhost:3000/login/success#accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
    }
}
