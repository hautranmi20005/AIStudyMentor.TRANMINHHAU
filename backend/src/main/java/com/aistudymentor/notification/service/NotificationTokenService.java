package com.aistudymentor.notification.service;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.notification.dto.DeviceTokenRequest;
import com.aistudymentor.notification.entity.DeviceToken;
import com.aistudymentor.notification.repository.DeviceTokenRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificationTokenService {

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("ANDROID", "IOS", "WEB");

    private final DeviceTokenRepository tokenRepository;
    private final StudentProfileRepository profileRepository;

    public NotificationTokenService(
            DeviceTokenRepository tokenRepository,
            StudentProfileRepository profileRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void register(UserPrincipal principal, DeviceTokenRequest request) {
        String platform = request.platform().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PLATFORMS.contains(platform)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported device platform.");
        }
        StudentProfile profile = getProfile(principal);
        DeviceToken device = tokenRepository.findByToken(request.token()).orElseGet(DeviceToken::new);
        device.setProfile(profile);
        device.setToken(request.token());
        device.setPlatform(platform);
        device.setAppVersion(request.appVersion());
        device.setLastSeen(LocalDateTime.now());
        tokenRepository.save(device);
    }

    @Transactional
    public void unregister(UserPrincipal principal, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        StudentProfile profile = getProfile(principal);
        tokenRepository.deleteByProfileAndToken(profile, token);
    }

    private StudentProfile getProfile(UserPrincipal principal) {
        return profileRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found."));
    }
}
