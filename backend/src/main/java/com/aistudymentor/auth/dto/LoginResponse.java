package com.aistudymentor.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private UUID userId;
    private String email;
    private boolean mfaRequired;
    private String mfaSessionToken;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
