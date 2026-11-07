package com.aistudymentor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class GeminiStructuredExplanation {

    @NotBlank(message = "Summary is required")
    @Size(min = 10, message = "Summary must be at least 10 characters")
    private String summary;

    @NotEmpty(message = "Explanation steps are required")
    @Valid
    private List<ExplanationStep> steps;

    @NotBlank(message = "Final answer is required")
    private String finalAnswer;

    @NotEmpty(message = "At least one key takeaway is required")
    private List<String> keyTakeaways;

    private String alternativeExplanation;
}
