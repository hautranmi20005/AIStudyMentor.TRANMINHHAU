package com.aistudymentor.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizEditResponseDto {

    private UUID quizId;
    private String subject;
    private String topic;
    private List<QuizQuestionEditDto> questions;
}
