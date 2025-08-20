package com.example.framework.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileMetadata store(MultipartFile file);
    void delete(Long fileId);
    FileMetadata get(Long fileId);
}