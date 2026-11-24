package com.aistudymentor.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaManagementRequest {
    @NotBlank
    private String password;

    @NotBlank
    private String code;
}
