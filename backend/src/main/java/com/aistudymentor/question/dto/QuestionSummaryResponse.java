package com.aistudymentor.question.dto;

import com.aistudymentor.question.entity.QuestionStatus;
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
public class QuestionSummaryResponse {

    private UUID questionId;
    private String subject;
    private String questionSnippet;
    private boolean hasImage;
    private boolean isBookmarked;
    private QuestionStatus status;
    private LocalDateTime createdAt;
}
