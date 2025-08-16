package com.example.framework.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        // JDK 설정이 낮아 .toList()가 안 되는 환경 대비 → Collectors.toList() 사용
        List<Map<String, Object>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toError) // 아래 helper
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("code", "VALIDATION_ERROR");
        body.put("message", "요청이 올바르지 않습니다");
        body.put("errors", errors);
        body.put("path", req.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> toError(FieldError fe) {
        Map<String, Object> m = new HashMap<>();
        m.put("field", fe.getField());
        // 메시지 커스터마이징 원하면 여기서 처리
        m.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        return m;
    }
}
