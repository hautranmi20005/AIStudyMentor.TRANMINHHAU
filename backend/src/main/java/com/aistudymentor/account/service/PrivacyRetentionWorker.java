package com.aistudymentor.account.service;

import com.aistudymentor.account.repository.GuardianConsentEvidenceRepository;
import com.aistudymentor.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PrivacyRetentionWorker {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final GuardianConsentEvidenceRepository consentEvidenceRepository;
    private final AccountPrivacyService accountPrivacyService;

    @Scheduled(fixedDelayString = "${app.privacy.retention-cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPrivacyData() {
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository
                .findTop100ByExpiresAtBeforeOrderByExpiresAtAsc(now)
                .forEach(passwordResetTokenRepository::delete);
        consentEvidenceRepository.deleteExpired(now);
        accountPrivacyService.cleanupPersistentEphemeralKeys();
    }
}
