package com.aistudymentor.notification.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRuleEvaluatorTest {

    @Test
    void underThirteenAccountsRequireApprovedGuardianConsent() {
        LocalDate today = LocalDate.of(2026, 5, 1);
        LocalDate underThirteen = LocalDate.of(2013, 5, 2);

        assertFalse(NotificationRuleEvaluator.isEligible(underThirteen, "ACTIVE", "PENDING", today));
        assertTrue(NotificationRuleEvaluator.isEligible(underThirteen, "ACTIVE", "APPROVED", today));
        assertTrue(NotificationRuleEvaluator.isEligible(
                LocalDate.of(2013, 5, 1), "ACTIVE", null, today));
        assertFalse(NotificationRuleEvaluator.isEligible(underThirteen, "SUSPENDED", "APPROVED", today));
    }

    @Test
    void quietHoursSupportNormalAndOvernightWindows() {
        assertTrue(NotificationRuleEvaluator.isQuietHours(
                LocalTime.of(22, 30), LocalTime.of(22, 0), LocalTime.of(7, 0)));
        assertTrue(NotificationRuleEvaluator.isQuietHours(
                LocalTime.of(6, 59), LocalTime.of(22, 0), LocalTime.of(7, 0)));
        assertFalse(NotificationRuleEvaluator.isQuietHours(
                LocalTime.of(7, 0), LocalTime.of(22, 0), LocalTime.of(7, 0)));
        assertFalse(NotificationRuleEvaluator.isQuietHours(
                LocalTime.NOON, LocalTime.of(22, 0), LocalTime.of(7, 0)));
    }
}
