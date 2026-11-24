package com.aistudymentor.common;

public final class CorrelationIds {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = CorrelationIds.class.getName() + ".value";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIds() {
    }
}
