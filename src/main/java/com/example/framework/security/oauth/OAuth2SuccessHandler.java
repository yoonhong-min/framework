package com.example.framework.security.oauth;

import com.example.framework.security.jwt.JwtService;
import com.example.framework.user.entity.User;
import com.example.framework.user.UserService;
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
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = (authentication instanceof OAuth2AuthenticationToken t)
                ? t.getAuthorizedClientRegistrationId() : "unknown";
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String externalId = null;
        String nickname = null;
        String email = null;

        if ("kakao".equals(registrationId)) {
            externalId = String.valueOf(attrs.get("id"));
            Object acc = attrs.get("kakao_account");
            if (acc instanceof Map<?,?> a) {
                Object prof = a.get("profile");
                if (prof instanceof Map<?,?> p && p.get("nickname") != null) {
                    nickname = String.valueOf(p.get("nickname"));
                }
                if (a.get("email") != null) email = String.valueOf(a.get("email"));
            }
        } else if ("naver".equals(registrationId)) {
            Object resp = attrs.get("response");
            if (resp instanceof Map<?,?> r) {
                externalId = String.valueOf(r.get("id"));
                Object nick = (r.get("nickname") != null) ? r.get("nickname") : r.get("name");
                if (nick != null) nickname = String.valueOf(nick);
                if (r.get("email") != null) email = String.valueOf(r.get("email"));
            }
        }

        if (externalId == null) {
            response.sendError(400, "OAuth2 profile missing id");
            return;
        }

        User user = userService.upsertOAuthUser(registrationId, externalId, email, nickname);

        var claims = new HashMap<String, Object>();
        claims.put("roles", user.getRoles());
        claims.put("tokenVersion", user.getTokenVersion());
        claims.put("nickname", user.getNickname());

        String accessToken  = jwtService.createAccessToken(String.valueOf(user.getId()), claims);
        String refreshToken = jwtService.createRefreshToken(String.valueOf(user.getId()), claims);

        Cookie refresh = new Cookie("REFRESH_TOKEN", refreshToken);
        refresh.setHttpOnly(true);
        refresh.setPath("/");
        refresh.setMaxAge(60 * 60 * 24 * 14);
        response.addCookie(refresh);

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
        {"provider":"%s","userId":%d,"nickname":%s,"accessToken":"%s"}
        """.formatted(
                registrationId,
                user.getId(),
                user.getNickname() == null ? "null" : "\"" + user.getNickname().replace("\"","\\\"") + "\"",
                accessToken
        ));
        response.getWriter().flush();
    }
}
