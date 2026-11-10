package com.aistudymentor.profile.service;

import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.dto.StudentProfileDto;
import com.aistudymentor.profile.dto.StudentProfileUpdateRequest;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;

    // ─────────────────────────── GET PROFILE ───────────────────────────

    @Cacheable(value = "user-profile", key = "#userId")
    @Transactional(readOnly = true)
    public StudentProfileDto getProfile(UUID userId) {
        StudentProfile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Profile not found for the authenticated user."));
        return toDto(profile);
    }

    // ─────────────────────────── UPDATE PROFILE ───────────────────────────

    @CacheEvict(value = "user-profile", key = "#userId")
    @Transactional
    public StudentProfileDto updateProfile(UUID userId, StudentProfileUpdateRequest request) {
        StudentProfile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Profile not found for the authenticated user."));

        if (request.getEducationLevel() != null) {
            profile.setEducationLevel(request.getEducationLevel());
        }
        if (request.getPreferredSubject() != null) {
            profile.setPreferredSubject(request.getPreferredSubject());
        }
        if (request.getLearningStyle() != null) {
            profile.setLearningStyle(request.getLearningStyle());
        }

        profile = profileRepository.save(profile);
        log.info("Profile updated for userId={}", userId);
        return toDto(profile);
    }

    // ─────────────────────────── HELPER ───────────────────────────

    private StudentProfileDto toDto(StudentProfile profile) {
        return StudentProfileDto.builder()
                .profileId(profile.getProfileId())
                .userId(profile.getUser().getUserId())
                .educationLevel(profile.getEducationLevel())
                .preferredSubject(profile.getPreferredSubject())
                .learningStyle(profile.getLearningStyle())
                .build();
    }
}
