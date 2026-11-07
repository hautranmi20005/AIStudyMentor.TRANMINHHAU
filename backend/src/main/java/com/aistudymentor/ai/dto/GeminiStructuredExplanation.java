package com.aistudymentor.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiStructuredExplanation {

    private String summary;
    private List<ExplanationStep> steps;
    private String finalAnswer;
    private List<String> keyTakeaways;
    private String alternativeExplanation;
}
