package com.aistudymentor.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 12, max = 72)
    private String newPassword;
}
