package com.example.framework.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties props;
    private final FileMetadataRepository repo;

    @Override
    public FileMetadata store(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("빈 파일입니다.");

        // 1) Content-Type 화이트리스트
        String ct = (file.getContentType() == null) ? "application/octet-stream" : file.getContentType();
        if (props.getAllowedContentTypes() != null &&
                !props.getAllowedContentTypes().contains(ct)) {
            throw new IllegalArgumentException("허용되지 않은 콘텐츠 타입: " + ct);
        }

        // 2) 저장 경로(타입별/날짜별 폴더)
        String ext = getExtension(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedName = uuid + (ext.isEmpty() ? "" : "." + ext);

        LocalDate today = LocalDate.now();
        String subDir = switch (ct.split("/")[0]) {
            case "image" -> "images";
            case "video" -> "videos";
            case "application", "text" -> "docs";
            default -> "misc";
        } + "/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue());

        Path base = Path.of(props.getBaseDir()).toAbsolutePath().normalize();
        Path destDir = base.resolve(subDir).normalize();

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패", e);
        }

        Path dest = destDir.resolve(storedName).normalize();

        // 3) 저장
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }

        // 4) 메타데이터 기록
        FileMetadata meta = new FileMetadata();
        meta.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename() == null ? storedName : file.getOriginalFilename()));
        meta.setStoredFilename(storedName);
        meta.setContentType(ct);
        meta.setSize(file.getSize());
        meta.setRelativePath(subDir + "/" + storedName);
        meta.setPublicUrl(props.getPublicUrlPrefix() + "/" + meta.getRelativePath());

        return repo.save(meta);
    }

    @Override
    public void delete(Long fileId) {
        var meta = repo.findById(fileId).orElseThrow();
        Path path = Path.of(props.getBaseDir()).resolve(meta.getRelativePath());
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
        repo.deleteById(fileId);
    }

    @Override
    public FileMetadata get(Long fileId) {
        return repo.findById(fileId).orElseThrow();
    }

    private static String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1).toLowerCase();
    }
}
