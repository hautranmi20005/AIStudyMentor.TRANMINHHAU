package com.aistudymentor.profile.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.profile.dto.StudentProfileDto;
import com.aistudymentor.profile.dto.StudentProfileUpdateRequest;
import com.aistudymentor.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * GET /api/v1/profile/me
     * PROTECTED — returns the authenticated student's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<StudentProfileDto> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.getUserId()));
    }

    /**
     * PATCH /api/v1/profile/me
     * PROTECTED — updates educationLevel, preferredSubject, learningStyle (all optional).
     */
    @PatchMapping("/me")
    public ResponseEntity<StudentProfileDto> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody StudentProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(principal.getUserId(), request));
    }
}
