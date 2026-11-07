package com.aistudymentor.question.dto;

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
public class QuestionBookmarkResponse {

    private UUID questionId;
    private boolean isBookmarked;
    private LocalDateTime bookmarkedAt;
}
