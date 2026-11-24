package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.DeviceToken;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.push", havingValue = "fcm")
public class FcmNotificationSender implements NotificationSender {

    private static final int MAX_TOKENS_PER_REQUEST = 500;
    private final FirebaseMessaging firebaseMessaging;

    public FcmNotificationSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public SendResult send(NotificationPayload payload, Collection<DeviceToken> devices) {
        List<DeviceToken> deviceList = List.copyOf(devices);
        int accepted = 0;
        Set<String> invalidTokens = new HashSet<>();
        for (int offset = 0; offset < deviceList.size(); offset += MAX_TOKENS_PER_REQUEST) {
            List<DeviceToken> batch = deviceList.subList(
                    offset, Math.min(offset + MAX_TOKENS_PER_REQUEST, deviceList.size()));
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch.stream().map(DeviceToken::getToken).toList())
                    .setNotification(Notification.builder()
                            .setTitle(payload.title())
                            .setBody(payload.body())
                            .build())
                    .putData("type", payload.type().name())
                    .putData("deepLink", payload.deepLink())
                    .build();
            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                accepted += response.getSuccessCount();
                for (int index = 0; index < response.getResponses().size(); index++) {
                    var item = response.getResponses().get(index);
                    FirebaseMessagingException exception = item.getException();
                    if (exception != null
                            && exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                        invalidTokens.add(batch.get(index).getToken());
                    }
                }
            } catch (FirebaseMessagingException exception) {
                throw new IllegalStateException("FCM notification delivery failed.", exception);
            }
        }
        return new SendResult(accepted, Set.copyOf(invalidTokens));
    }
}
