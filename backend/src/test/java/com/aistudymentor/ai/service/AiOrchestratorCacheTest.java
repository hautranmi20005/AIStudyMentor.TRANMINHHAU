package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.AiQuotaResponse;
import com.aistudymentor.ai.dto.AnswerRequest;
import com.aistudymentor.ai.dto.ExplanationStep;
import com.aistudymentor.ai.dto.StructuredAnswer;
import com.aistudymentor.ai.metrics.AiMetrics;
import com.aistudymentor.profile.entity.StudentProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiOrchestratorCacheTest {

    @Test
    void cacheHitSkipsProviderAndDailyQuotaReservation() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.modelId()).thenReturn("gemini-test");
        AiResponseCache cache = mock(AiResponseCache.class);
        UUID profileId = UUID.randomUUID();
        when(cache.keyForUser(eq(profileId), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                nullable(String.class), nullable(byte[].class), anyInt()))
                .thenReturn("cache-key");
        when(cache.get("cache-key", StructuredAnswer.class)).thenReturn(validAnswer());
        AiQuotaService quotas = mock(AiQuotaService.class);
        when(quotas.remaining(profileId)).thenReturn(AiQuotaResponse.builder()
                .remainingCallsToday(7L)
                .remainingTokensToday(9000L)
                .build());

        var validatorFactory = Validation.buildDefaultValidatorFactory();
        try {
            AiOrchestrator orchestrator = new AiOrchestrator(
                    provider, cache, new AiMetrics(new SimpleMeterRegistry()),
                    new AiSafetyClassifier(), new PromptTemplateService(), quotas,
                    mock(AiUsageLogger.class), new ObjectMapper(), validatorFactory.getValidator(), 8);
            StudentProfile profile = StudentProfile.builder().profileId(profileId).build();
            AnswerRequest request = AnswerRequest.builder()
                    .subject("mathematics")
                    .questionText("Explain fractions")
                    .educationLevel("middle-school")
                    .explanationStyle("step-by-step")
                    .answerLanguage("en")
                    .build();

            var execution = orchestrator.answer(profile, request, UUID.randomUUID());

            assertTrue(execution.value().getCacheHit());
            assertEquals(0, execution.value().getInputTokens());
            assertEquals(0, execution.value().getOutputTokens());
            assertEquals(7, execution.remainingCallsToday());
            verify(provider, never()).answer(any());
            verify(quotas, never()).reserve(profileId);
        } finally {
            validatorFactory.close();
        }
    }

    private StructuredAnswer validAnswer() {
        return StructuredAnswer.builder()
                .subject("mathematics")
                .difficulty("middle-school")
                .summary("Fractions represent parts of a whole.")
                .steps(List.of(ExplanationStep.builder()
                        .stepNumber(1)
                        .title("Identify the parts")
                        .explanation("The numerator counts selected parts.")
                        .build()))
                .finalAnswer("A fraction represents part of a whole.")
                .keyConcepts(List.of("numerator"))
                .commonMistakes(List.of("Mixing numerator and denominator"))
                .followUpQuestions(List.of("How do equivalent fractions work?"))
                .inputTokens(20)
                .outputTokens(30)
                .build();
    }
}
