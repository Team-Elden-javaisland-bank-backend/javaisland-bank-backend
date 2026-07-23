package com.javaisland.bank_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Locale;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public record ErrorResponseDto(
            LocalDateTime timestamp,
            int status,
            String errorCode,
            String message
    ) {}

    @ExceptionHandler(ApiBankException.class)
    public ResponseEntity<ErrorResponseDto> handleApiBankException(ApiBankException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String message = messageSource.getMessage(ex.getErrorCode(), null, ex.getMessage(), locale);

        log.warn("Business rule violation: [{}] {}", ex.getErrorCode(), message);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getErrorCode(), message);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(messageSource.getMessage("VALIDATION_ERROR", null, "Validation failed", locale));

        log.warn("Validation failed: {}", fieldErrors);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", fieldErrors);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String violations = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(messageSource.getMessage("VALIDATION_ERROR", null, "Validation failed", locale));

        log.warn("Entity validation failed: {}", violations);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", violations);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        String message = messageSource.getMessage("INTERNAL_ERROR", null, "An unexpected error occurred. Please try again later.", locale);

        log.error("Unexpected error", ex);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR", message);
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                return Locale.forLanguageTag(acceptLanguage.split(",")[0].trim());
            } catch (Exception e) {
                // fallback to Italian
            }
        }
        return Locale.ITALIAN;
    }
}
