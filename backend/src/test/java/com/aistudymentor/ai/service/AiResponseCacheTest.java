package com.aistudymentor.ai.service;

import com.aistudymentor.ai.metrics.AiMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiResponseCacheTest {

    private final AiResponseCache cache = new AiResponseCache(
            null, new ObjectMapper(), new AiMetrics(new SimpleMeterRegistry()),
            Duration.ofDays(7), "test");

    @Test
    void normalizesUnicodeWhitespaceAndCaseForAcademicQuestions() {
        assertEquals(key("  CAFÉ \t  is   what? ", "mathematics"),
                key("cafe\u0301 is what?", "MATHEMATICS"));
    }

    @Test
    void preservesProgrammingQuestionCase() {
        assertNotEquals(key("What does Foo do?", "programming"),
                key("What does foo do?", "programming"));
    }

    @Test
    void scopesCachedResponsesToTheProfileForDeletion() {
        UUID firstProfile = UUID.randomUUID();
        UUID secondProfile = UUID.randomUUID();

        assertNotEquals(
                userKey(firstProfile, "Explain fractions"),
                userKey(secondProfile, "Explain fractions"));
        assertTrue(userKey(firstProfile, "Explain fractions")
                .startsWith("studymentor:ai:user:" + firstProfile + ":"));
    }

    @Test
    void separatesEntriesByEducationLevelStyleLanguagePromptAndModel() {
        String base = key("Explain fractions", "mathematics");
        assertNotEquals(base, key("Explain fractions", "mathematics", "college", "step-by-step", "en", "answer/v1", "model-a", null, null, 0));
        assertNotEquals(base, key("Explain fractions", "mathematics", "middle-school", "short", "en", "answer/v1", "model-a", null, null, 0));
        assertNotEquals(base, key("Explain fractions", "mathematics", "middle-school", "step-by-step", "fr", "answer/v1", "model-a", null, null, 0));
        assertNotEquals(base, key("Explain fractions", "mathematics", "middle-school", "step-by-step", "en", "answer/v2", "model-a", null, null, 0));
        assertNotEquals(base, key("Explain fractions", "mathematics", "middle-school", "step-by-step", "en", "answer/v1", "model-b", null, null, 0));
    }

    @Test
    void separatesTaskOutputCountAndImageFingerprint() {
        String base = key("Explain fractions", "mathematics");
        assertNotEquals(base, key("Explain fractions", "mathematics", "middle-school", "step-by-step", "auto", "quiz/v1", "model-a", null, null, 5));
        assertNotEquals(key("Explain fractions", "mathematics", "middle-school", "step-by-step", "auto", "quiz/v1", "model-a", null, null, 5),
                key("Explain fractions", "mathematics", "middle-school", "step-by-step", "auto", "quiz/v1", "model-a", null, null, 10));
        assertNotEquals(imageKey(new byte[]{1, 2, 3}), imageKey(new byte[]{1, 2, 4}));
    }

    private String key(String question, String subject) {
        return key(question, subject, "middle-school", "step-by-step", "auto",
                "answer/v1", "model-a", null, null, 0);
    }

    private String imageKey(byte[] image) {
        return key("What is shown?", "biology", "middle-school", "step-by-step", "auto",
                "answer/v1", "model-a", "image/png", image, 0);
    }

    private String userKey(UUID profileId, String question) {
        return cache.keyForUser(profileId, question, "mathematics", "middle-school", "step-by-step",
                "under-18", "en", "answer", "answer/v1", "model-a", null, null, 0);
    }

    private String key(
            String question,
            String subject,
            String level,
            String style,
            String language,
            String prompt,
            String model,
            String mime,
            byte[] image,
            int outputCount) {
        return cache.key(question, subject, level, style, "under-18", language,
                prompt.startsWith("quiz") ? "quiz" : "answer", prompt, model, mime, image, outputCount);
    }
}
