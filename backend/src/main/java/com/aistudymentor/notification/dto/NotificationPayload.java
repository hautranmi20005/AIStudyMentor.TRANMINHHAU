package com.aistudymentor.notification.dto;

import com.aistudymentor.notification.entity.NotificationType;

public record NotificationPayload(
        NotificationType type,
        String title,
        String body,
        String deepLink
) {
}
