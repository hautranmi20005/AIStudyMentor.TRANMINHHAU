package com.aistudymentor.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionEditDto {

    @NotBlank(message = "question prompt is required")
    @Size(max = 2000, message = "question prompt cannot exceed 2000 characters")
    private String prompt;

    @NotEmpty(message = "question options are required")
    @Size(min = 3, max = 5, message = "multiple-choice questions must have 3 to 5 options")
    private List<@NotBlank @Size(max = 255) String> options;

    @NotBlank(message = "correct answer is required")
    @Size(max = 255, message = "correct answer cannot exceed 255 characters")
    private String correctAnswer;

    @NotBlank(message = "question explanation is required")
    @Size(max = 2000, message = "question explanation cannot exceed 2000 characters")
    private String explanation;
}
