package com.aistudymentor.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DashboardBucket(
        String label,
        LocalDate periodStart,
        LocalDateTime storageStartInclusive,
        LocalDateTime storageEndExclusive
) {
}
