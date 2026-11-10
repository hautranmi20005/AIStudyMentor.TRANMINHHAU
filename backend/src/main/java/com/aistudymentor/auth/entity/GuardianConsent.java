package com.aistudymentor.auth.entity;

import com.aistudymentor.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guardian_consents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianConsent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "consent_id", updatable = false, nullable = false, length = 36)
    private UUID consentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "guardian_email", nullable = false, length = 150)
    private String guardianEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "resend_count", nullable = false)
    @Builder.Default
    private Integer resendCount = 0;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "approved_ip", length = 45)
    private String approvedIp;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
