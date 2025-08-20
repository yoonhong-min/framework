package com.example.framework.user;

import com.example.framework.user.entity.OAuthAccount;
import com.example.framework.user.entity.User;
import com.example.framework.user.repository.OAuthAccountRepository;
import com.example.framework.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerLocal(String email, String rawPassword, String nickname) {
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        });
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .roles(Set.of("ROLE_USER"))
                .tokenVersion(1)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User loginLocal(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    @Transactional
    public User upsertOAuthUser(String provider, String providerUserId, String email, String displayName) {
        return oauthRepo.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .orElseGet(() -> {
                    // 동일 email이 이미 로컬로 존재하면 연결(선택 정책) / 없으면 신규 생성
                    User base = userRepository.findByEmail(email).orElseGet(() ->
                            userRepository.save(User.builder()
                                    .email(email)
                                    .passwordHash(null)
                                    .nickname(displayName)
                                    .roles(Set.of("ROLE_USER"))
                                    .tokenVersion(1)
                                    .build())
                    );
                    OAuthAccount link = OAuthAccount.builder()
                            .provider(provider)
                            .providerUserId(providerUserId)
                            .user(base)
                            .emailFromProvider(email)
                            .displayName(displayName)
                            .build();
                    oauthRepo.save(link);
                    return base;
                });
    }

    public User findByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}