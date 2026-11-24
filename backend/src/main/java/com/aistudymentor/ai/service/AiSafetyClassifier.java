package com.aistudymentor.ai.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AiSafetyClassifier {

    private static final Pattern INJECTION_OR_EXFILTRATION = Pattern.compile(
            "(?is)\\b(ignore|disregard|override)\\b.{0,60}\\b(previous|above|system|all)\\b"
                    + "|\\b(reveal|print|exfiltrate|repeat)\\b.{0,60}"
                    + "\\b(system prompt|hidden prompt|hidden instructions|api key|secret)\\b");
    private static final Pattern UNSAFE_REQUEST = Pattern.compile(
            "(?is)\\b(build|make|create|weaponize|poison|explosive|bomb)\\b.{0,80}"
                    + "\\b(weapon|bomb|explosive|poison|attack|harm|kill)\\b"
                    + "|\\b(poison|harm|kill|hurt|attack)\\b.{0,50}"
                    + "\\b(person|classmate|teacher|someone|people|student)\\b");
    private static final Pattern NON_ACADEMIC = Pattern.compile(
            "(?is)^\\s*(tell me (?:a joke|.{0,30}\\bweather\\b)|write (?:a|an) .{0,30}"
                    + "\\b(?:love letter|wedding speech)\\b|plan my vacation|give me a recipe|"
                    + "what is the weather)\\b");

    public Classification classify(String text) {
        if (text == null || text.isBlank()) {
            return Classification.INVALID;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (INJECTION_OR_EXFILTRATION.matcher(normalized).find()
                || UNSAFE_REQUEST.matcher(normalized).find()
                || NON_ACADEMIC.matcher(normalized).find()) {
            return Classification.OUT_OF_SCOPE;
        }
        return Classification.ACADEMIC;
    }

    public enum Classification {
        ACADEMIC,
        OUT_OF_SCOPE,
        INVALID
    }
}
