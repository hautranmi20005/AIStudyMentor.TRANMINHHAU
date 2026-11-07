package com.aistudymentor.auth.dto;

import com.aistudymentor.auth.entity.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianConsentResponse {

    private UUID consentId;
    private UUID userId;
    private String guardianEmail;
    private ConsentStatus status;
    private LocalDateTime verifiedAt;
    private boolean canAccessCoreFeatures;
}
