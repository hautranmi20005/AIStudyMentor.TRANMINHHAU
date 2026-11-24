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
public class GeneratedQuizQuestion {

    private String type;

    private String prompt;

    private List<String> options;

    private String correctAnswer;

    private List<String> acceptedAnswers;

    private String explanation;

    private String topic;
}
