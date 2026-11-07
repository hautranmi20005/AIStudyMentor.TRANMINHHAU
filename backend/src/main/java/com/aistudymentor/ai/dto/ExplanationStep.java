package com.aistudymentor.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationStep {

    private Integer stepNumber;
    private String title;
    private String explanation;
    private String formula;
}
