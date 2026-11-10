package com.aistudymentor.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAuthorizeResponse {

    private String firebaseStoragePath;
    private String uploadToken;
}
