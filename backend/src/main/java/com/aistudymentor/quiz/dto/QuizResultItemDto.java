package com.aistudymentor.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultItemDto {

    private UUID questionId;
    private Boolean isCorrect;
    private String correctAnswer;
    private String explanation;
}
