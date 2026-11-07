package com.aistudymentor.question.dto;

import com.aistudymentor.question.entity.QuestionStatus;
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
public class QuestionResponse {

    private UUID questionId;
    private String subject;
    private String questionText;
    private QuestionStatus status;
    private boolean isBookmarked;
    private List<String> imageStoragePaths;
    private AnswerResponse answer;
    private LocalDateTime createdAt;
}
