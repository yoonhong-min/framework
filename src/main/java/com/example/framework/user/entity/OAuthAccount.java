package com.example.framework.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
@Table(name="oauth_accounts",
        uniqueConstraints = @UniqueConstraint(name="uk_provider_pid", columnNames = {"provider","providerUserId"}))
public class OAuthAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=30, nullable=false)
    private String provider;         // kakao, naver

    @Column(length=100, nullable=false)
    private String providerUserId;   // 카카오 id, 네이버 id

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id")
    private User user;

    private String emailFromProvider; // 제공되면 저장
    private String displayName;       // 닉네임 등
}