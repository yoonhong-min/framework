package com.example.framework.file;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "files")
@Getter @Setter
public class FileMetadata {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String originalFilename;

    @Column(nullable=false, unique=true)
    private String storedFilename;     // 실제 디스크 파일명 (UUID 등)

    @Column(nullable=false)
    private String contentType;

    @Column(nullable=false)
    private long size;

    @Column(nullable=false)
    private String relativePath;       // baseDir 기준 하위 경로 (예: images/2025/08/hello.png)

    @Column(nullable=false)
    private String publicUrl;          // /files/... 로 접근 가능한 URL

    private Instant uploadedAt = Instant.now();

    // 필요 시 업로더 식별자, 태그, 해시 등 추가 가능
}