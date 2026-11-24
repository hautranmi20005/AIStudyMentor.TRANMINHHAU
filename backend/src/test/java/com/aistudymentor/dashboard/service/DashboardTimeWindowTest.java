package com.aistudymentor.dashboard.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardTimeWindowTest {

    @Test
    void buildsLocalCalendarDayBoundsForAsiaTimezone() {
        DashboardTimeWindow window = DashboardTimeWindow.resolve(
                DashboardRange.SEVEN_DAYS,
                ZoneId.of("Asia/Ho_Chi_Minh"),
                ZoneOffset.UTC,
                Instant.parse("2026-11-14T16:30:00Z"));

        assertEquals(Instant.parse("2026-11-07T17:00:00Z"), window.startInclusive());
        assertEquals(Instant.parse("2026-11-14T17:00:00Z"), window.endExclusive());
        assertEquals(LocalDate.parse("2026-11-08"), window.startDate());
    }

    @Test
    void calendarWindowTracksDaylightSavingOffsetChanges() {
        DashboardTimeWindow window = DashboardTimeWindow.resolve(
                DashboardRange.SEVEN_DAYS,
                ZoneId.of("America/Los_Angeles"),
                ZoneOffset.UTC,
                Instant.parse("2026-03-08T12:00:00Z"));

        assertEquals(Instant.parse("2026-03-02T08:00:00Z"), window.startInclusive());
        assertEquals(Instant.parse("2026-03-09T07:00:00Z"), window.endExclusive());
        assertEquals(Duration.ofHours(167), Duration.between(window.startInclusive(), window.endExclusive()));
    }
}
