package com.aistudymentor.notification.dto;

import java.time.LocalTime;

public record NotificationPreferencesDto(
        boolean bookmarkReviewEnabled,
        boolean repeatedMistakeEnabled,
        boolean weeklySummaryEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        String timezone
) {
}
