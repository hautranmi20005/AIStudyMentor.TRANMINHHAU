package com.aistudymentor.notification.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.notification.dto.DeviceTokenDeleteRequest;
import com.aistudymentor.notification.dto.DeviceTokenRequest;
import com.aistudymentor.notification.dto.NotificationPreferencesDto;
import com.aistudymentor.notification.service.NotificationPreferenceService;
import com.aistudymentor.notification.service.NotificationTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationTokenService tokenService;
    private final NotificationPreferenceService preferenceService;

    @PutMapping("/devices/token")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        tokenService.register(principal, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/devices/token")
    public ResponseEntity<Void> deleteDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeviceTokenDeleteRequest request
    ) {
        tokenService.unregister(principal, request.token());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferencesDto> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(preferenceService.get(principal));
    }

    @PutMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferencesDto> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferencesDto request
    ) {
        return ResponseEntity.ok(preferenceService.update(principal, request));
    }
}
