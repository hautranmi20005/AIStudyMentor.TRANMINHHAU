package com.aistudymentor.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizCreateRequest {

    @NotBlank(message = "subject is required")
    private String subject;

    @Min(value = 1, message = "numQuestions must be at least 1")
    @Max(value = 20, message = "numQuestions cannot exceed 20")
    @Builder.Default
    private Integer numQuestions = 5;
}
