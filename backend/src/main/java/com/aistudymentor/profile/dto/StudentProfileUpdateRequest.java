package com.aistudymentor.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileUpdateRequest {

    private String educationLevel;
    private String preferredSubject;
    private String learningStyle;
}
