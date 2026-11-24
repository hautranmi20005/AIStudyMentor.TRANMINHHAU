package com.aistudymentor.notification.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.dto.NotificationTestRequest;
import com.aistudymentor.notification.entity.NotificationType;
import com.aistudymentor.notification.service.LocalNotificationSender;
import com.aistudymentor.notification.service.NotificationDeliveryService;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dev/notifications")
@Profile("dev")
public class DevNotificationController {

    private final String adminKey;
    private final NotificationDeliveryService deliveryService;
    private final StudentProfileRepository profileRepository;
    private final ObjectProvider<LocalNotificationSender> localSenderProvider;

    public DevNotificationController(
            @Value("${app.push-dev-admin-key:}") String adminKey,
            NotificationDeliveryService deliveryService,
            StudentProfileRepository profileRepository,
            ObjectProvider<LocalNotificationSender> localSenderProvider
    ) {
        this.adminKey = adminKey;
        this.deliveryService = deliveryService;
        this.profileRepository = profileRepository;
        this.localSenderProvider = localSenderProvider;
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationPayload> sendTest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-Dev-Admin-Key", required = false) String suppliedKey,
            @Valid @RequestBody NotificationTestRequest request
    ) {
        requireAdminKey(suppliedKey);
        UUID profileId = profileRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found."))
                .getProfileId();
        NotificationPayload payload = payloadFor(request.type());
        deliveryService.enqueue(profileId, request.type(), "manual-" + UUID.randomUUID(), payload);
        deliveryService.deliverPending();
        return ResponseEntity.accepted().body(payload);
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<NotificationPayload>> getLocalOutbox(
            @RequestHeader(name = "X-Dev-Admin-Key", required = false) String suppliedKey
    ) {
        requireAdminKey(suppliedKey);
        LocalNotificationSender localSender = localSenderProvider.getIfAvailable();
        if (localSender == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Local notification sender is not enabled.");
        }
        return ResponseEntity.ok(localSender.getOutbox());
    }

    private void requireAdminKey(String suppliedKey) {
        if (adminKey.isBlank() || suppliedKey == null || !MessageDigest.isEqual(
                adminKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dev notification admin authorization required.");
        }
    }

    private NotificationPayload payloadFor(NotificationType type) {
        return switch (type) {
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
    }
}
