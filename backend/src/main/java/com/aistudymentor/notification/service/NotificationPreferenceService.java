package com.aistudymentor.notification.service;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.notification.dto.NotificationPreferencesDto;
import com.aistudymentor.notification.entity.NotificationPreference;
import com.aistudymentor.notification.repository.NotificationPreferenceRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final StudentProfileRepository profileRepository;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            StudentProfileRepository profileRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.profileRepository = profileRepository;
    }

    public NotificationPreferencesDto get(UserPrincipal principal) {
        UUID profileId = getProfile(principal).getProfileId();
        return preferenceRepository.findByProfile_ProfileId(profileId)
                .map(NotificationPreferenceService::toDto)
                .orElseGet(NotificationPreferenceService::defaults);
    }

    @Transactional
    public NotificationPreferencesDto update(UserPrincipal principal, NotificationPreferencesDto request) {
        if ((request.quietHoursStart() == null) != (request.quietHoursEnd() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both quiet-hours boundaries must be provided together.");
        }
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid IANA timezone is required.");
        }
        StudentProfile profile = getProfile(principal);
        NotificationPreference preference = preferenceRepository.findById(profile.getProfileId())
                .orElseGet(NotificationPreference::new);
        preference.setProfileId(profile.getProfileId());
        preference.setProfile(profile);
        preference.setBookmarkReviewEnabled(request.bookmarkReviewEnabled());
        preference.setRepeatedMistakeEnabled(request.repeatedMistakeEnabled());
        preference.setWeeklySummaryEnabled(request.weeklySummaryEnabled());
        preference.setQuietHoursStart(request.quietHoursStart());
        preference.setQuietHoursEnd(request.quietHoursEnd());
        preference.setTimezone(request.timezone());
        return toDto(preferenceRepository.save(preference));
    }

    public Optional<NotificationPreference> find(UUID profileId) {
        return preferenceRepository.findByProfile_ProfileId(profileId);
    }

    public static NotificationPreferencesDto toDto(NotificationPreference preference) {
        return new NotificationPreferencesDto(
                preference.isBookmarkReviewEnabled(),
                preference.isRepeatedMistakeEnabled(),
                preference.isWeeklySummaryEnabled(),
                preference.getQuietHoursStart(),
                preference.getQuietHoursEnd(),
                preference.getTimezone());
    }

    public static NotificationPreferencesDto defaults() {
        return new NotificationPreferencesDto(true, true, true, null, null, "UTC");
    }

    private StudentProfile getProfile(UserPrincipal principal) {
        return profileRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found."));
    }
}
