package com.example.framework.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.Set;

@Entity @Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
@Table(name = "users", indexes = {
        @Index(name="uk_user_email", columnList = "email", unique = true)
})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=120, unique = true)
    private String email;          // 소셜만 쓰면 null 가능

    private String passwordHash;   // 소셜만 쓰면 null 가능

    @Column(length=40)
    private String nickname;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="user_roles", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role")
    private Set<String> roles;     // e.g. ROLE_USER, ROLE_ADMIN

    private Integer tokenVersion;  // 강제 로그아웃/토큰 무효화에 사용

    @CreationTimestamp
    private Instant createdAt;
}
