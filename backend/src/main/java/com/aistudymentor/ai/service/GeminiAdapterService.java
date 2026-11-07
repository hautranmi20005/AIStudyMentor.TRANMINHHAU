package com.aistudymentor.ai.service;

import com.aistudymentor.common.exception.AiNotConfiguredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class GeminiAdapterService {

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:}")
    private String model;

    @Value("${app.gemini.api-version:}")
    private String apiVersion;

    /**
     * Decision D3: Fail-fast with AI_NOT_CONFIGURED error code when model/api are unconfigured or placeholder.
     */
    public void validateAiConfiguration() {
        if (!StringUtils.hasText(apiKey) || "<SET_ME>".equals(apiKey)
                || !StringUtils.hasText(model) || "<SET_ME>".equals(model)
                || !StringUtils.hasText(apiVersion) || "<SET_ME>".equals(apiVersion)) {
            log.error("AI service invoked but Google Gemini credentials/model are unconfigured.");
            throw new AiNotConfiguredException("Google Gemini AI is not configured on this environment (AI_NOT_CONFIGURED).");
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && !"<SET_ME>".equals(apiKey)
                && StringUtils.hasText(model) && !"<SET_ME>".equals(model)
                && StringUtils.hasText(apiVersion) && !"<SET_ME>".equals(apiVersion);
    }
}
