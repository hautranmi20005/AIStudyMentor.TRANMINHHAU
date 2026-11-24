package com.aistudymentor.notification.service;

import com.aistudymentor.notification.dto.NotificationPayload;
import com.aistudymentor.notification.entity.DeviceToken;
import com.aistudymentor.notification.entity.NotificationDelivery;
import com.aistudymentor.notification.entity.NotificationType;
import com.aistudymentor.notification.repository.DeviceTokenRepository;
import com.aistudymentor.notification.repository.NotificationDeliveryRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final DeviceTokenRepository tokenRepository;
    private final StudentProfileRepository profileRepository;
    private final NotificationSender sender;
    private final ObjectMapper objectMapper;

    public NotificationDeliveryService(
            NotificationDeliveryRepository deliveryRepository,
            DeviceTokenRepository tokenRepository,
            StudentProfileRepository profileRepository,
            NotificationSender sender,
            ObjectMapper objectMapper
    ) {
        this.deliveryRepository = deliveryRepository;
        this.tokenRepository = tokenRepository;
        this.profileRepository = profileRepository;
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean enqueue(UUID profileId, NotificationType type, String periodKey, NotificationPayload payload) {
        if (deliveryRepository.existsByProfile_ProfileIdAndTypeAndPeriodKey(profileId, type, periodKey)) {
            return false;
        }
        StudentProfile profile = profileRepository.getReferenceById(profileId);
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setProfile(profile);
        delivery.setType(type);
        delivery.setPeriodKey(periodKey);
        try {
            delivery.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize notification payload.", exception);
        }
        deliveryRepository.saveAndFlush(delivery);
        return true;
    }

    public int deliverPending() {
        List<NotificationDelivery> deliveries = deliveryRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");
        int sentCount = 0;
        for (NotificationDelivery delivery : deliveries) {
            NotificationPayload payload = deserialize(delivery.getPayloadJson());
            List<DeviceToken> devices = tokenRepository.findByProfile_ProfileId(
                    delivery.getProfile().getProfileId());
            NotificationSender.SendResult result = sender.send(payload, devices);
            result.invalidTokens().forEach(tokenRepository::deleteByToken);
            if (result.acceptedCount() > 0) {
                delivery.setStatus("SENT");
                delivery.setSentAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                sentCount++;
            }
        }
        return sentCount;
    }

    private NotificationPayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, NotificationPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read persisted notification payload.", exception);
        }
    }
}
