package com.aistudymentor.quiz.dto;

import com.aistudymentor.quiz.entity.QuizStatus;
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
public class QuizResponseDto {

    private UUID quizId;
    private String subject;
    private QuizStatus status;
    private List<QuizQuestionDto> questions;
    private LocalDateTime createdAt;
}
