package com.aistudymentor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("geminiConfiguration")
public class GeminiConfigurationHealthIndicator implements HealthIndicator {

    private final boolean configured;

    public GeminiConfigurationHealthIndicator(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:}") String model,
            @Value("${app.gemini.api-version:}") String apiVersion) {
        configured = StringUtils.hasText(apiKey) && !"<SET_ME>".equals(apiKey)
                && StringUtils.hasText(model) && !"<SET_ME>".equals(model)
                && StringUtils.hasText(apiVersion) && !"<SET_ME>".equals(apiVersion);
    }

    @Override
    public Health health() {
        return Health.up().withDetail("configured", configured).build();
    }
}
