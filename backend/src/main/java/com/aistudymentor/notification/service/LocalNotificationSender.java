package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.DeviceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(name = "app.push", havingValue = "local", matchIfMissing = true)
public class LocalNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationSender.class);
    private final CopyOnWriteArrayList<NotificationPayload> outbox = new CopyOnWriteArrayList<>();

    @Override
    public SendResult send(NotificationPayload payload, Collection<DeviceToken> devices) {
        outbox.add(payload);
        log.info("Local notification intended: type={}, route={}", payload.type(), payload.deepLink());
        return new SendResult(1, java.util.Set.of());
    }

    public List<NotificationPayload> getOutbox() {
        return List.copyOf(outbox);
    }
}
