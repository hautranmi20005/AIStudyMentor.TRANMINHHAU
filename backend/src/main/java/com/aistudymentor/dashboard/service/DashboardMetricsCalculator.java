package com.aistudymentor.dashboard.service;

import com.aistudymentor.dashboard.dto.DashboardMetricsResponse;
import com.aistudymentor.dashboard.repository.DashboardAggregationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DashboardMetricsCalculator {

    private DashboardMetricsCalculator() {
    }

    public static double accuracyPercent(long correctCount, long gradedCount) {
        if (gradedCount <= 0) {
            return 0.0;
        }
        return roundPercent(correctCount * 100.0 / gradedCount);
    }

    public static double roundPercent(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static List<DashboardMetricsResponse.SubjectMetric> topSubjectsWithOther(
            List<DashboardAggregationRepository.SubjectCount> counts,
            long totalQuestions,
            int topN
    ) {
        List<DashboardAggregationRepository.SubjectCount> sorted = counts.stream()
                .sorted(Comparator.comparingLong(DashboardAggregationRepository.SubjectCount::count).reversed()
                        .thenComparing(DashboardAggregationRepository.SubjectCount::subject))
                .limit(topN)
                .toList();
        List<DashboardMetricsResponse.SubjectMetric> result = new ArrayList<>();
        long included = 0;
        for (DashboardAggregationRepository.SubjectCount count : sorted) {
            result.add(new DashboardMetricsResponse.SubjectMetric(count.subject(), count.count()));
            included += count.count();
        }
        long other = Math.max(0, totalQuestions - included);
        if (other > 0) {
            result.add(new DashboardMetricsResponse.SubjectMetric("Other", other));
        }
        return List.copyOf(result);
    }

    public static List<DashboardMetricsResponse.Insight> accuracyInsights(
            List<DashboardAggregationRepository.SubjectBucketAccuracy> points
    ) {
        Map<String, List<DashboardAggregationRepository.SubjectBucketAccuracy>> bySubject = points.stream()
                .filter(point -> point.gradedCount() > 0)
                .collect(Collectors.groupingBy(
                        DashboardAggregationRepository.SubjectBucketAccuracy::subject,
                        HashMap::new,
                        Collectors.toList()));
        List<DashboardMetricsResponse.Insight> insights = new ArrayList<>();
        bySubject.forEach((subject, values) -> {
            List<DashboardAggregationRepository.SubjectBucketAccuracy> ordered = values.stream()
                    .sorted(Comparator.comparing(DashboardAggregationRepository.SubjectBucketAccuracy::label))
                    .toList();
            if (ordered.size() < 2) {
                return;
            }
            DashboardAggregationRepository.SubjectBucketAccuracy first = ordered.get(0);
            DashboardAggregationRepository.SubjectBucketAccuracy last = ordered.get(ordered.size() - 1);
            double firstPercent = accuracyPercent(first.correctCount(), first.gradedCount());
            double lastPercent = accuracyPercent(last.correctCount(), last.gradedCount());
            if (firstPercent == lastPercent) {
                return;
            }
            boolean improved = lastPercent > firstPercent;
            String message = String.format(
                    java.util.Locale.ROOT,
                    "Your %s accuracy %s from %.1f%% to %.1f%% between weeks %s and %s.",
                    subject,
                    improved ? "improved" : "changed",
                    firstPercent,
                    lastPercent,
                    first.label(),
                    last.label());
            insights.add(new DashboardMetricsResponse.Insight(
                    improved ? "ACCURACY_IMPROVED" : "ACCURACY_CHANGED",
                    message,
                    first.label(),
                    first.correctCount(),
                    first.gradedCount(),
                    firstPercent,
                    last.label(),
                    last.correctCount(),
                    last.gradedCount(),
                    lastPercent));
        });
        return insights.stream()
                .sorted(Comparator
                        .comparingDouble((DashboardMetricsResponse.Insight insight) ->
                                Math.abs(insight.currentAccuracyPercent() - insight.previousAccuracyPercent()))
                        .reversed()
                        .thenComparing(DashboardMetricsResponse.Insight::message))
                .limit(3)
                .toList();
    }
}
