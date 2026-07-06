// 1. L'eccezione custom
package com.javaisland.bank_backend.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

public class ApiBankException extends RuntimeException {
    public ApiBankException(String message) {
        super(message);
    }
}
