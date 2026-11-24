package com.aistudymentor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuiz {

    @NotEmpty
    @Valid
    private List<GeneratedQuizQuestion> questions;

    private String modelId;
    private String promptVersion;
    private Integer inputTokens;
    private Integer outputTokens;
}
