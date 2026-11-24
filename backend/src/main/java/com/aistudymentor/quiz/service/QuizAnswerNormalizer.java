package com.aistudymentor.quiz.service;

import java.text.Normalizer;
import java.util.Locale;

public final class QuizAnswerNormalizer {

    private QuizAnswerNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return Normalizer.normalize(decomposed.replaceAll("\\p{M}+", ""), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\p{javaWhitespace}+", " ");
    }
}
