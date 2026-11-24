package com.aistudymentor.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountReauthenticationRequest {
    @NotBlank
    private String password;

    @NotBlank
    private String code;
}
