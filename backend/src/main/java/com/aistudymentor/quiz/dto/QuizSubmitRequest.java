package com.aistudymentor.quiz.dto;

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
public class QuizSubmitRequest {

    @NotEmpty(message = "responses list cannot be empty")
    @Valid
    private List<QuizResponseItemDto> responses;
}
