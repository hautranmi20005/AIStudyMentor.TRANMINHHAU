package com.aistudymentor.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(max = 20) String platform,
        @Size(max = 50) String appVersion
) {
}
