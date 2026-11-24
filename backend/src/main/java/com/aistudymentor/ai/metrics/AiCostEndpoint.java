package com.aistudymentor.ai.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "ai-cost")
@RequiredArgsConstructor
public class AiCostEndpoint {

    private final AiMetrics metrics;

    @ReadOperation
    public AiMetrics.AiCostReport report() {
        return metrics.costReport();
    }
}
