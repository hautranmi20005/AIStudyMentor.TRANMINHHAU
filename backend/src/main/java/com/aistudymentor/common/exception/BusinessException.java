package com.aistudymentor.common.exception;

import org.springframework.http.HttpStatus;

/**
 * General-purpose domain exception that carries an HTTP status, an application error code,
 * and a human-readable message. Handled globally by {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
