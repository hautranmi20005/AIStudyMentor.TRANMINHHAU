package com.aistudymentor.quiz.dto;

import com.aistudymentor.quiz.entity.QuizQuestionType;
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
public class QuizQuestionDto {

    private UUID questionId;
    private QuizQuestionType type;
    private String prompt;
    private List<QuizOptionDto> options;
}
