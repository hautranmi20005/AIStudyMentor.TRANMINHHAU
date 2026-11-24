package com.aistudymentor.question.dto;

import jakarta.validation.constraints.NotNull;

public record AlternativeExplanationRequest(@NotNull AlternativeExplanationType type) {
}
