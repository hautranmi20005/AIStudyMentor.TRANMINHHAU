package com.aistudymentor.common.exception;

public class AiNotConfiguredException extends RuntimeException {
    public static final String ERROR_CODE = "AI_NOT_CONFIGURED";

    public AiNotConfiguredException(String message) {
        super(message);
    }
}
