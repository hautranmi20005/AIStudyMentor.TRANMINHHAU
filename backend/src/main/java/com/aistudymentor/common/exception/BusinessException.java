package com.aistudymentor.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * General-purpose domain exception that carries an HTTP status, an application error code,
 * and a human-readable message. Handled globally by {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? null : Map.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
