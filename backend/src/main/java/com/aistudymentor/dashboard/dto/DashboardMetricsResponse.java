package com.aistudymentor.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

    private Long totalQuestionsAsked;
    private Long totalQuizzesCompleted;
    private Double overallAccuracyRate;
    private List<SubjectBreakdownDto> subjectBreakdown;
}
