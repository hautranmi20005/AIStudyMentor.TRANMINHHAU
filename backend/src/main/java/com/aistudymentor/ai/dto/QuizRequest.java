package com.aistudymentor.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class QuizRequest {

    @NotBlank
    @Size(max = 100)
    private String subject;

    @NotBlank
    @Size(max = 50)
    private String educationLevel;

    @Min(1)
    @Max(20)
    @Builder.Default
    private int numQuestions = 5;

    @Builder.Default
    private String explanationStyle = "step-by-step";

    @Builder.Default
    private String answerLanguage = "auto";

    @Builder.Default
    private String ageGroup = "under-18";

    @Size(max = 100)
    private String topic;

    @Size(max = 50)
    private String sourceType;

    @Size(max = 2000)
    private String sourceQuestion;

    @Size(max = 4000)
    private String sourceAnswer;

    @Builder.Default
    private boolean shortAnswerEnabled = false;

    @Size(max = 500)
    private String repairHint;
}
