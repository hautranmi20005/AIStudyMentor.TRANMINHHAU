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

    public record SubjectMetric(String subject, long count) {
    }

    public record Insight(
            String type,
            String message,
            String previousLabel,
            long previousCorrectCount,
            long previousGradedCount,
            double previousAccuracyPercent,
            String currentLabel,
            long currentCorrectCount,
            long currentGradedCount,
            double currentAccuracyPercent
    ) {
    }
}
