package com.aistudymentor.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardian_consent_evidence")
@Getter
@Setter
@NoArgsConstructor
public class GuardianConsentEvidence {

    @Id
    @Column(name = "user_id_hash", nullable = false, length = 64)
    private String userIdHash;

    @Column(name = "consent_status", nullable = false, length = 30)
    private String consentStatus;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public GuardianConsentEvidence(
            String userIdHash,
            String consentStatus,
            LocalDateTime consentAt,
            LocalDateTime expiresAt) {
        this.userIdHash = userIdHash;
        this.consentStatus = consentStatus;
        this.consentAt = consentAt;
        this.expiresAt = expiresAt;
    }
}
