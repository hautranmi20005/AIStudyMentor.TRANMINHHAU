package com.aistudymentor.notification.service;

import com.aistudymentor.notification.entity.NotificationPreference;
import com.aistudymentor.notification.entity.NotificationType;
import com.aistudymentor.notification.repository.NotificationPreferenceRepository;
import com.aistudymentor.notification.repository.NotificationRuleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSchedulerTest {

    @Test
    void appliesNotificationPreferencesAndSkipsUnderThirteenWithoutConsent() {
        UUID enabledProfile = UUID.randomUUID();
        UUID restrictedProfile = UUID.randomUUID();
        NotificationRuleRepository rules = mock(NotificationRuleRepository.class);
        NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
        NotificationDeliveryService deliveries = mock(NotificationDeliveryService.class);
        when(rules.findProfiles()).thenReturn(List.of(
                new NotificationRuleRepository.ProfileRecord(
                        enabledProfile, LocalDate.now().minusYears(18), "ACTIVE", null),
                new NotificationRuleRepository.ProfileRecord(
                        restrictedProfile, LocalDate.now().minusYears(12), "ACTIVE", "PENDING")));
        when(rules.findBookmarkReminderProfiles(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(enabledProfile, restrictedProfile));
        when(rules.findRepeatedMistakeProfiles(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(enabledProfile, restrictedProfile));
        when(rules.findRecentlyActiveProfiles(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(enabledProfile, restrictedProfile));
        NotificationPreference preference = new NotificationPreference();
        preference.setProfileId(enabledProfile);
        preference.setTimezone("UTC");
        preference.setBookmarkReviewEnabled(false);
        preference.setRepeatedMistakeEnabled(true);
        preference.setWeeklySummaryEnabled(false);
        when(preferences.findAll()).thenReturn(List.of(preference));

        new NotificationScheduler(rules, preferences, deliveries).evaluateRules();

        verify(deliveries).enqueue(
                eq(enabledProfile),
                eq(NotificationType.REPEATED_MISTAKE),
                anyString(),
                argThat(payload -> payload.type() == NotificationType.REPEATED_MISTAKE
                        && payload.body().equals("A short practice session may help reinforce your learning.")));
        verify(deliveries, never()).enqueue(
                eq(enabledProfile), eq(NotificationType.BOOKMARK_REVIEW), anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(deliveries, never()).enqueue(
                eq(enabledProfile), eq(NotificationType.WEEKLY_SUMMARY), anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(deliveries, never()).enqueue(
                eq(restrictedProfile), org.mockito.ArgumentMatchers.any(), anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(deliveries).deliverPending();
    }
}
