package com.aistudymentor.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitResponse {

    private UUID attemptId;
    private Integer score;
    private Integer totalQuestions;
    private Integer correctCount;
    private List<QuizResultItemDto> results;
    private LocalDateTime submittedAt;
}
