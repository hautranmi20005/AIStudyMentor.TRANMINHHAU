package com.aistudymentor.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptStartResponse {

    private UUID attemptId;
    private UUID quizId;
    private LocalDateTime startTime;
}
