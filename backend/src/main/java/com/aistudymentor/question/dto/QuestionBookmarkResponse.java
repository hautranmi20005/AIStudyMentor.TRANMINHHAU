package com.aistudymentor.question.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    private LocalDateTime bookmarkedAt;
}
