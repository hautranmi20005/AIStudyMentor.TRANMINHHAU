package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.DeviceToken;

import java.util.Collection;
import java.util.Set;

public interface NotificationSender {
    SendResult send(NotificationPayload payload, Collection<DeviceToken> devices);

    record SendResult(int acceptedCount, Set<String> invalidTokens) {
    }
}
