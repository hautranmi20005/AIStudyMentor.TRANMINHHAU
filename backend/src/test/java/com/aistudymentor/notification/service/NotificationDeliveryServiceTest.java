package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.DeviceToken;
import com.aistudymentor.notification.entity.NotificationDelivery;
import com.aistudymentor.notification.entity.NotificationType;
import com.aistudymentor.notification.repository.DeviceTokenRepository;
import com.aistudymentor.notification.repository.NotificationDeliveryRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryServiceTest {

    @Test
    void duplicatePeriodIsNotEnqueuedTwice() {
        NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        StudentProfileRepository profiles = mock(StudentProfileRepository.class);
        NotificationSender sender = (payload, devices) -> new NotificationSender.SendResult(0, Set.of());
        UUID profileId = UUID.randomUUID();
        when(deliveries.existsByProfile_ProfileIdAndTypeAndPeriodKey(
                profileId, NotificationType.BOOKMARK_REVIEW, "2026-W20")).thenReturn(true);
        NotificationDeliveryService service = new NotificationDeliveryService(
                deliveries, tokens, profiles, sender, new ObjectMapper());

        boolean enqueued = service.enqueue(
                profileId,
                NotificationType.BOOKMARK_REVIEW,
                "2026-W20",
                new NotificationPayload(
                        NotificationType.BOOKMARK_REVIEW, "Study reminder", "Generic reminder.",
                        "studymentor://bookmarks"));

        org.junit.jupiter.api.Assertions.assertFalse(enqueued);
        verify(deliveries, never()).saveAndFlush(any());
        verify(profiles, never()).getReferenceById(profileId);
    }

    @Test
    void removesTokensReportedAsInvalidBySender() throws Exception {
        NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
        DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
        StudentProfileRepository profiles = mock(StudentProfileRepository.class);
        String invalidToken = "fcm-token";
        UUID profileId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setProfileId(profileId);
        DeviceToken device = new DeviceToken();
        device.setToken(invalidToken);
        NotificationPayload payload = new NotificationPayload(
                NotificationType.WEEKLY_SUMMARY, "Weekly study summary", "Generic summary.",
                "studymentor://dashboard");
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setProfile(profile);
        delivery.setType(NotificationType.WEEKLY_SUMMARY);
        delivery.setPayloadJson(new ObjectMapper().writeValueAsString(payload));
        when(deliveries.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(delivery));
        when(tokens.findByProfile_ProfileId(profileId)).thenReturn(List.of(device));
        NotificationSender sender = (ignored, devices) ->
                new NotificationSender.SendResult(0, Set.of(invalidToken));
        NotificationDeliveryService service = new NotificationDeliveryService(
                deliveries, tokens, profiles, sender, new ObjectMapper());

        service.deliverPending();

        verify(tokens).deleteByToken(invalidToken);
    }
}
