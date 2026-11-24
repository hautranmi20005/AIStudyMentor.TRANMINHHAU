package com.aistudymentor.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

    @NotBlank
    @Size(max = 100)
    private String subject;

    @Size(max = 8000)
    private String questionText;

    @NotBlank
    @Size(max = 50)
    private String educationLevel;

    @Builder.Default
    private String explanationStyle = "step-by-step";

    @Builder.Default
    private String ageGroup = "under-18";

    @Builder.Default
    private String answerLanguage = "auto";

    private byte[] imageBytes;
    private String imageMimeType;

    @Builder.Default
    private boolean alternative = false;

    @Builder.Default
    private String alternativeType = "different-approach";
}
