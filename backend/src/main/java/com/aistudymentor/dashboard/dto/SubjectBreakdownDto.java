package com.aistudymentor.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectBreakdownDto {

    private String subject;
    private Long questionsCount;
    private Long quizCount;
    private Double accuracyRate;
}
