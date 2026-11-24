package com.aistudymentor.config;

import com.aistudymentor.ai.service.GeminiAdapterService;
import com.aistudymentor.common.exception.AiNotConfiguredException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class GeminiStartupGuardTest {

    @Test
    void missingGeminiConfigurationDoesNotAbortStartup() {
        GeminiAdapterService adapter = mock(GeminiAdapterService.class);
        doThrow(new AiNotConfiguredException("not configured"))
                .when(adapter).validateAiConfiguration();

        GeminiStartupGuard guard = new GeminiStartupGuard(adapter);

        assertDoesNotThrow(() -> guard.run(new DefaultApplicationArguments()));
    }
}
