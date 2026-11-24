package com.aistudymentor.config;

import com.aistudymentor.ai.service.GeminiAdapterService;
import com.aistudymentor.common.exception.AiNotConfiguredException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Profile({"dev", "prod"})
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiStartupGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeminiStartupGuard.class);
    private final GeminiAdapterService adapter;

    public GeminiStartupGuard(GeminiAdapterService adapter) {
        this.adapter = adapter;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            adapter.validateAiConfiguration();
        } catch (AiNotConfiguredException exception) {
            log.warn("Gemini is not configured; backend startup will continue, but AI requests will return "
                    + "AI_NOT_CONFIGURED. Set GEMINI_API_KEY to enable Ask AI.");
        }
    }
}
