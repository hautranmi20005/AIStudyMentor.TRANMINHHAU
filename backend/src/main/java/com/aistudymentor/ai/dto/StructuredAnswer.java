package com.aistudymentor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredAnswer {

    @NotBlank
    private String subject;

    @NotBlank
    private String difficulty;

    @NotBlank
    @Size(min = 10)
    private String summary;

    @NotEmpty
    @Valid
    private List<ExplanationStep> steps;

    @NotBlank
    private String finalAnswer;

    @NotEmpty
    private List<@NotBlank String> keyConcepts;

    @NotEmpty
    private List<@NotBlank String> commonMistakes;

    @NotEmpty
    private List<@NotBlank String> followUpQuestions;

    private String alternativeExplanation;
    private String modelId;
    private String promptVersion;
    private Integer inputTokens;
    private Integer outputTokens;

    @Builder.Default
    private Boolean cacheHit = false;

    private Long remainingCallsToday;
}
