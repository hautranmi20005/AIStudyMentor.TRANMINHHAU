package com.aistudymentor.notification.repository;

import com.aistudymentor.notification.entity.DeviceToken;
import com.aistudymentor.profile.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByToken(String token);
    List<DeviceToken> findByProfile(StudentProfile profile);
    List<DeviceToken> findByProfile_ProfileId(UUID profileId);
    void deleteByProfileAndToken(StudentProfile profile, String token);
    void deleteByToken(String token);
    void deleteByProfile_ProfileId(UUID profileId);
}
