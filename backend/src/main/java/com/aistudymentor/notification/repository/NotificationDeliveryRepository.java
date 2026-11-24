package com.aistudymentor.notification.repository;

import com.aistudymentor.notification.entity.NotificationDelivery;
import com.aistudymentor.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    boolean existsByProfile_ProfileIdAndTypeAndPeriodKey(UUID profileId, NotificationType type, String periodKey);
    List<NotificationDelivery> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
