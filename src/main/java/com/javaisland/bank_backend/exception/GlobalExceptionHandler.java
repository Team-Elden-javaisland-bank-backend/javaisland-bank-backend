package com.javaisland.bank_backend.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponseDto(
            LocalDateTime timestamp,
            int status,
            String errorCode,
            String message
    ) {}

    @ExceptionHandler(ApiBankException.class)
    public ResponseEntity<ErrorResponseDto> handleApiBankException(ApiBankException ex) {
        log.warn("Business rule violation: [{}] {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation failed: {}", fieldErrors);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", fieldErrors);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex) {
        String violations = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Entity validation failed: {}", violations);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", violations);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR", "An unexpected technical error occurred. Please try again later.");
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
