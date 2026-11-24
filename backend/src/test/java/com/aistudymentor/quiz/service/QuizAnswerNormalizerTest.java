package com.aistudymentor.quiz.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QuizAnswerNormalizerTest {

    @Test
    void ignoresCaseWhitespaceAndDiacriticsButPreservesPunctuation() {
        assertEquals("cafe noir", QuizAnswerNormalizer.normalize("  CAFÉ \t Noir  "));
        assertEquals(QuizAnswerNormalizer.normalize("résumé"),
                QuizAnswerNormalizer.normalize("RESUME"));
        assertNotEquals(QuizAnswerNormalizer.normalize("1/2"),
                QuizAnswerNormalizer.normalize("1 2"));
    }
}
