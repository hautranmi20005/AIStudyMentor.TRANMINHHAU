package com.aistudymentor.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "subject cannot exceed 100 characters")
    private String subject;

    @Size(max = 100, message = "topic cannot exceed 100 characters")
    private String topic;

    @Min(value = 1, message = "numQuestions must be at least 1")
    @Max(value = 10, message = "numQuestions cannot exceed 10")
    @Builder.Default
    private Integer numQuestions = 5;
}
