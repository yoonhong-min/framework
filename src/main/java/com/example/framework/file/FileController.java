package com.example.framework.file;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storage;

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> upload(@RequestPart("file") @NotNull MultipartFile file) {
        return ResponseEntity.ok(storage.store(file));
    }

    @PostMapping("/upload-multi")
    public ResponseEntity<List<FileMetadata>> uploadMulti(@RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(files.stream().map(storage::store).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileMetadata> get(@PathVariable Long id) {
        return ResponseEntity.ok(storage.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storage.delete(id);
        return ResponseEntity.noContent().build();
    }
}
