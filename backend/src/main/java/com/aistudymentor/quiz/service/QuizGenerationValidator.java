package com.aistudymentor.quiz.service;

import com.aistudymentor.ai.dto.GeneratedQuizQuestion;
import com.aistudymentor.quiz.entity.QuizQuestionType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuizGenerationValidator {

    public boolean isValid(GeneratedQuizQuestion question, boolean shortAnswerEnabled) {
        if (question == null || !StringUtils.hasText(question.getPrompt())
                || !StringUtils.hasText(question.getCorrectAnswer())
                || !StringUtils.hasText(question.getExplanation())
                || question.getPrompt().length() > 2000
                || question.getCorrectAnswer().length() > 255
                || question.getExplanation().length() > 2000) {
            return false;
        }

        QuizQuestionType type;
        try {
            type = QuizQuestionType.valueOf(question.getType());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }

        List<String> options = question.getOptions() == null ? List.of() : question.getOptions();
        List<String> accepted = question.getAcceptedAnswers() == null
                ? List.of() : question.getAcceptedAnswers();
        if (options.stream().anyMatch(value -> !StringUtils.hasText(value))
                || accepted.stream().anyMatch(value -> !StringUtils.hasText(value))
                || options.stream().anyMatch(value -> value.length() > 255)
                || accepted.stream().anyMatch(value -> value.length() > 255)) {
            return false;
        }

        return switch (type) {
            case MCQ -> validMultipleChoice(question.getCorrectAnswer(), options);
            case FILL_IN_THE_BLANK -> validFillInTheBlank(question.getCorrectAnswer(), options, accepted);
            case SHORT_ANSWER -> shortAnswerEnabled
                    && options.isEmpty()
                    && acceptedAnswersIncludeCorrect(question.getCorrectAnswer(), accepted);
        };
    }

    private boolean validMultipleChoice(String correctAnswer, List<String> options) {
        if (options.size() < 3 || options.size() > 5 || !areDistinct(options)) {
            return false;
        }
        String normalizedCorrect = QuizAnswerNormalizer.normalize(correctAnswer);
        return options.stream()
                .filter(option -> QuizAnswerNormalizer.normalize(option).equals(normalizedCorrect))
                .count() == 1;
    }

    private boolean validFillInTheBlank(String correctAnswer, List<String> options, List<String> accepted) {
        return options.isEmpty() && !accepted.isEmpty() && accepted.size() <= 8
                && areDistinct(accepted) && acceptedAnswersIncludeCorrect(correctAnswer, accepted);
    }

    private boolean acceptedAnswersIncludeCorrect(String correctAnswer, List<String> accepted) {
        String normalizedCorrect = QuizAnswerNormalizer.normalize(correctAnswer);
        return !normalizedCorrect.isEmpty() && accepted.stream()
                .anyMatch(answer -> QuizAnswerNormalizer.normalize(answer).equals(normalizedCorrect));
    }

    private boolean areDistinct(List<String> values) {
        Set<String> normalized = values.stream()
                .map(QuizAnswerNormalizer::normalize)
                .collect(Collectors.toSet());
        return normalized.size() == values.size();
    }
}
