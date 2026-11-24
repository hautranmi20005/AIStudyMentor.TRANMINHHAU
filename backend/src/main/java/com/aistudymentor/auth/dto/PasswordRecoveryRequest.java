package com.aistudymentor.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordRecoveryRequest {
    @NotBlank
    @Email
    private String email;
}
