package com.aistudymentor.question.dto;

import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
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
public class AnswerResponse {

    private UUID answerId;
    private boolean isAlternative;
    private GeminiStructuredExplanation structuredExplanation;
    private LocalDateTime createdAt;
}
