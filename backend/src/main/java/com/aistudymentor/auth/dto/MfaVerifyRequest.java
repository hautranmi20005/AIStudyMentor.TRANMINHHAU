package com.aistudymentor.auth.dto;

import com.aistudymentor.auth.entity.MfaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    @NotBlank(message = "MFA session token is required")
    private String mfaSessionToken;

    @NotBlank(message = "Verification code is required")
    private String code;

    @NotNull(message = "MFA factor type is required")
    private MfaType type;
}
