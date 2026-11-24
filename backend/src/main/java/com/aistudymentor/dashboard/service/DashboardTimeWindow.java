package com.aistudymentor.dashboard.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public record DashboardTimeWindow(
        DashboardRange range,
        ZoneId zone,
        LocalDate startDate,
        LocalDate endExclusiveDate,
        Instant startInclusive,
        Instant endExclusive,
        LocalDateTime storageStartInclusive,
        LocalDateTime storageEndExclusive
) {
    public static DashboardTimeWindow resolve(
            DashboardRange range,
            ZoneId zone,
            ZoneId storageZone,
            Instant now
    ) {
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate startDate = range == DashboardRange.ALL ? null : today.minusDays(range.days() - 1L);
        LocalDate endExclusiveDate = today.plusDays(1);
        Instant start = startDate == null ? null : startDate.atStartOfDay(zone).toInstant();
        Instant end = endExclusiveDate.atStartOfDay(zone).toInstant();
        return new DashboardTimeWindow(
                range,
                zone,
                startDate,
                endExclusiveDate,
                start,
                end,
                toStorageDateTime(start, storageZone),
                toStorageDateTime(end, storageZone));
    }

    public static DashboardTimeWindow resolveLookback(
            int days,
            ZoneId zone,
            ZoneId storageZone,
            Instant now
    ) {
        if (days < 1) {
            throw new IllegalArgumentException("Dashboard lookback days must be at least one.");
        }
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDate endExclusiveDate = today.plusDays(1);
        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endExclusiveDate.atStartOfDay(zone).toInstant();
        return new DashboardTimeWindow(
                DashboardRange.THIRTY_DAYS,
                zone,
                startDate,
                endExclusiveDate,
                start,
                end,
                toStorageDateTime(start, storageZone),
                toStorageDateTime(end, storageZone));
    }

    public static LocalDateTime toStorageDateTime(Instant instant, ZoneId storageZone) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, storageZone);
    }

    public static Instant toInstant(LocalDateTime storageDateTime, ZoneId storageZone) {
        return storageDateTime == null ? null : storageDateTime.atZone(storageZone).toInstant();
    }

    public DashboardBucket bucket(String label, LocalDate periodStart, LocalDate startDate, LocalDate endDate,
                                  ZoneId storageZone) {
        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endDate.atStartOfDay(zone).toInstant();
        return new DashboardBucket(
                label,
                periodStart,
                toStorageDateTime(start, storageZone),
                toStorageDateTime(end, storageZone));
    }

    public java.util.List<DashboardBucket> dailyBuckets(LocalDate firstDate, ZoneId storageZone) {
        if (firstDate == null || !firstDate.isBefore(endExclusiveDate)) {
            return java.util.List.of();
        }
        java.util.List<DashboardBucket> buckets = new java.util.ArrayList<>();
        for (LocalDate day = firstDate; day.isBefore(endExclusiveDate); day = day.plusDays(1)) {
            buckets.add(bucket(day.toString(), day, day, day.plusDays(1), storageZone));
        }
        return java.util.List.copyOf(buckets);
    }

    public java.util.List<DashboardBucket> weeklyBuckets(LocalDate firstDate, ZoneId storageZone) {
        if (firstDate == null || !firstDate.isBefore(endExclusiveDate)) {
            return java.util.List.of();
        }
        java.util.List<DashboardBucket> buckets = new java.util.ArrayList<>();
        LocalDate monday = firstDate.with(java.time.DayOfWeek.MONDAY);
        while (monday.isBefore(endExclusiveDate)) {
            LocalDate start = monday.isBefore(firstDate) ? firstDate : monday;
            LocalDate nextMonday = monday.plusWeeks(1);
            LocalDate end = nextMonday.isAfter(endExclusiveDate) ? endExclusiveDate : nextMonday;
            buckets.add(bucket(monday.toString(), monday, start, end, storageZone));
            monday = nextMonday;
        }
        return java.util.List.copyOf(buckets);
    }

    public LocalDate localDateOfStorageTimestamp(LocalDateTime timestamp, ZoneId storageZone) {
        Instant instant = toInstant(timestamp, storageZone);
        return instant == null ? null : instant.atZone(zone).toLocalDate();
    }

}
