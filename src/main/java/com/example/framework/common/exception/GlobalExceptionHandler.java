package com.example.framework.common.exception;

import com.example.framework.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.error("BAD_REQUEST", ex.getBindingResult().toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<?> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.error("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ApiResponse.error(ex.getCode().name(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleOthers(Exception ex) {
        return ApiResponse.error("INTERNAL_ERROR", ex.getMessage());
    }
}