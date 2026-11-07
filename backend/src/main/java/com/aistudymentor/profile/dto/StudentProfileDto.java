package com.aistudymentor.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {

    private UUID profileId;
    private UUID userId;
    private String educationLevel;
    private String preferredSubject;
    private String learningStyle;
}
