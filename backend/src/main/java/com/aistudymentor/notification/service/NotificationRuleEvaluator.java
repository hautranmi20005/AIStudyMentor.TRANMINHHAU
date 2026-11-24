package com.aistudymentor.notification.service;

import java.time.LocalDate;
import java.time.LocalTime;

public final class NotificationRuleEvaluator {

    private NotificationRuleEvaluator() {
    }

    public static boolean isEligible(LocalDate dateOfBirth, String accountStatus, String consentStatus, LocalDate today) {
        if (dateOfBirth == null || !"ACTIVE".equals(accountStatus)) {
            return false;
        }
        boolean underThirteen = dateOfBirth.plusYears(13).isAfter(today);
        return !underThirteen || "APPROVED".equals(consentStatus);
    }

    public static boolean isQuietHours(LocalTime current, LocalTime start, LocalTime end) {
        if (start == null || end == null || start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }
}
