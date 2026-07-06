package com.javaisland.bank_backend.exception;

import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiBankException.class)
    public ResponseEntity<Map<String, String>> handleApiBankException(ApiBankException ex) {
        // Restituisce un JSON pulito con il messaggio d'errore richiesto
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}