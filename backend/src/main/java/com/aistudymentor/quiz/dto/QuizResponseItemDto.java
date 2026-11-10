package com.aistudymentor.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponseItemDto {

    @NotNull(message = "questionId is required")
    private UUID questionId;

    private UUID selectedOptionId;
    private String subjectiveText;
}
