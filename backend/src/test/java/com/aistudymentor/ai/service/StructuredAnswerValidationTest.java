package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.ExplanationStep;
import com.aistudymentor.ai.dto.StructuredAnswer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredAnswerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsCompleteStructuredAnswer() {
        assertTrue(validator.validate(validAnswer()).isEmpty());
    }

    @Test
    void rejectsMissingFieldsAndEmptySteps() {
        StructuredAnswer answer = validAnswer();
        answer.setSubject(null);
        answer.setSteps(List.of());

        assertFalse(validator.validate(answer).isEmpty());
    }

    @Test
    void rejectsWrongJsonType() {
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> objectMapper.readValue("{\"steps\":\"not-an-array\"}", StructuredAnswer.class));
    }

    private StructuredAnswer validAnswer() {
        return StructuredAnswer.builder()
                .subject("Mathematics")
                .difficulty("introductory")
                .summary("A complete explanation for a student.")
                .steps(List.of(ExplanationStep.builder()
                        .stepNumber(1)
                        .title("Isolate x")
                        .explanation("Subtract two from both sides.")
                        .build()))
                .finalAnswer("x = 5")
                .keyConcepts(List.of("Inverse operations"))
                .commonMistakes(List.of("Changing one side only"))
                .followUpQuestions(List.of("How can the answer be checked?"))
                .build();
    }
}
