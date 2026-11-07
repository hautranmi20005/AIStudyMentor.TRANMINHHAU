package com.aistudymentor.auth.dto;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID userId;
    private String email;
    private AccountStatus accountStatus;
    private ConsentStatus consentStatus;
    private boolean mfaRequired;
    private boolean guardianConsentRequired;
    private String message;
}
