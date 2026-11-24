package com.aistudymentor.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAuthorizeResponse {

    private UUID uploadId;
    private String uploadUrl;
    private String storagePath;
    private Instant expiresAt;
    private String firebaseStoragePath;
    private String uploadToken;
}
