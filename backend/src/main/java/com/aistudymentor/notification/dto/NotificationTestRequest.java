package com.aistudymentor.notification.dto;

import com.aistudymentor.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;

public record NotificationTestRequest(@NotNull NotificationType type) {
}
