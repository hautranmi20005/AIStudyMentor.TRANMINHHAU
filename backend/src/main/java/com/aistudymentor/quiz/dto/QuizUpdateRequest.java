package com.aistudymentor.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizUpdateRequest {

    @NotBlank(message = "subject is required")
    @Size(max = 100, message = "subject cannot exceed 100 characters")
    private String subject;

    @Size(max = 100, message = "topic cannot exceed 100 characters")
    private String topic;

    @Size(min = 1, max = 10, message = "a quiz must contain between 1 and 10 questions")
    private List<@Valid QuizQuestionEditDto> questions;
}
