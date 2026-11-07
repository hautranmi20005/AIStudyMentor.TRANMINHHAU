package com.aistudymentor.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplanationStep {

    @NotNull(message = "stepNumber is required")
    @Min(value = 1, message = "stepNumber must be at least 1")
    private Integer stepNumber;

    @NotBlank(message = "Step title is required")
    @Size(min = 3, message = "Step title must have at least 3 characters")
    private String title;

    @NotBlank(message = "Step explanation is required")
    @Size(min = 5, message = "Step explanation must have at least 5 characters")
    private String explanation;

    private String formula;
}
