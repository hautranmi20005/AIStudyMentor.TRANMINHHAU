package com.aistudymentor.auth.repository;

import com.aistudymentor.auth.entity.GuardianConsent;
import com.aistudymentor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, UUID> {
    Optional<GuardianConsent> findByUser(User user);
    Optional<GuardianConsent> findByUser_UserId(UUID userId);
}
