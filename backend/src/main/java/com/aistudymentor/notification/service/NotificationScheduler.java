package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.NotificationPreference;
import com.aistudymentor.notification.entity.NotificationType;
import com.aistudymentor.notification.repository.NotificationPreferenceRepository;
import com.aistudymentor.notification.repository.NotificationRuleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationScheduler {

    private static final int BOOKMARK_REVIEW_AFTER_DAYS = 3;
    private static final int MISTAKE_LOOKBACK_DAYS = 30;
    private static final int WEEKLY_ACTIVITY_LOOKBACK_DAYS = 7;

    private final NotificationRuleRepository ruleRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDeliveryService deliveryService;

    public NotificationScheduler(
            NotificationRuleRepository ruleRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationDeliveryService deliveryService
    ) {
        this.ruleRepository = ruleRepository;
        this.preferenceRepository = preferenceRepository;
        this.deliveryService = deliveryService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void evaluateRules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        Map<UUID, NotificationRuleRepository.ProfileRecord> eligibleProfiles = new HashMap<>();
        ruleRepository.findProfiles().stream()
                .filter(profile -> NotificationRuleEvaluator.isEligible(
                        profile.dateOfBirth(), profile.accountStatus(), profile.consentStatus(), today))
                .forEach(profile -> eligibleProfiles.put(profile.profileId(), profile));

        Map<UUID, NotificationPreference> preferences = new HashMap<>();
        preferenceRepository.findAll()
                .forEach(preference -> preferences.put(preference.getProfileId(), preference));

        Set<UUID> bookmarkProfiles = new HashSet<>(
                ruleRepository.findBookmarkReminderProfiles(now.minusDays(BOOKMARK_REVIEW_AFTER_DAYS)));
        Set<UUID> mistakeProfiles = new HashSet<>(
                ruleRepository.findRepeatedMistakeProfiles(now.minusDays(MISTAKE_LOOKBACK_DAYS)));
        Set<UUID> activeProfiles = new HashSet<>(
                ruleRepository.findRecentlyActiveProfiles(now.minusDays(WEEKLY_ACTIVITY_LOOKBACK_DAYS)));

        for (UUID profileId : eligibleProfiles.keySet()) {
            NotificationPreference preference = preferences.get(profileId);
            ZoneId zone = zone(preference);
            ZonedDateTime localNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone);
            if (isQuiet(localNow, preference)) {
                continue;
            }
            String periodKey = weekKey(localNow);
            if (bookmarkProfiles.contains(profileId) && enabled(preference, NotificationType.BOOKMARK_REVIEW)) {
                queue(profileId, NotificationType.BOOKMARK_REVIEW, periodKey);
            }
            if (mistakeProfiles.contains(profileId) && enabled(preference, NotificationType.REPEATED_MISTAKE)) {
                queue(profileId, NotificationType.REPEATED_MISTAKE, periodKey);
            }
            if (activeProfiles.contains(profileId)
                    && isWeeklySummaryWindow(localNow)
                    && enabled(preference, NotificationType.WEEKLY_SUMMARY)) {
                queue(profileId, NotificationType.WEEKLY_SUMMARY, periodKey);
            }
        }
        deliveryService.deliverPending();
    }

    private void queue(UUID profileId, NotificationType type, String periodKey) {
        NotificationPayload payload = switch (type) {
            case BOOKMARK_REVIEW -> new NotificationPayload(
                    type, "Study reminder", "Your saved study materials are ready to review.",
                    "studymentor://bookmarks");
            case REPEATED_MISTAKE -> new NotificationPayload(
                    type, "Practice reminder", "A short practice session may help reinforce your learning.",
                    "studymentor://quizzes");
            case WEEKLY_SUMMARY -> new NotificationPayload(
                    type, "Weekly study summary", "Your weekly learning summary is ready.",
                    "studymentor://dashboard");
        };
        deliveryService.enqueue(profileId, type, periodKey, payload);
    }

    private boolean enabled(NotificationPreference preference, NotificationType type) {
        if (preference == null) {
            return true;
        }
        return switch (type) {
            case BOOKMARK_REVIEW -> preference.isBookmarkReviewEnabled();
            case REPEATED_MISTAKE -> preference.isRepeatedMistakeEnabled();
            case WEEKLY_SUMMARY -> preference.isWeeklySummaryEnabled();
        };
    }

    private ZoneId zone(NotificationPreference preference) {
        if (preference == null) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(preference.getTimezone());
    }

    private boolean isQuiet(ZonedDateTime localNow, NotificationPreference preference) {
        return preference != null && NotificationRuleEvaluator.isQuietHours(
                localNow.toLocalTime(), preference.getQuietHoursStart(), preference.getQuietHoursEnd());
    }

    private boolean isWeeklySummaryWindow(ZonedDateTime localNow) {
        return (localNow.getDayOfWeek().getValue() == 1 || localNow.getDayOfWeek().getValue() == 2)
                && localNow.getHour() >= 9 && localNow.getHour() < 21;
    }

    private String weekKey(ZonedDateTime localNow) {
        return localNow.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                + String.format("%02d", localNow.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
