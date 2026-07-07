package com.javaisland.bank_backend.exception;

public class ApiBankException extends RuntimeException {

    private final String errorCode;

    public ApiBankException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
    }

    public ApiBankException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
