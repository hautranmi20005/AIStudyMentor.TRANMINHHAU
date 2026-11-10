package com.aistudymentor.auth.service;

import com.aistudymentor.auth.entity.MfaAttempt;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.MfaAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaRateLimitService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    private final MfaAttemptRepository mfaAttemptRepository;

    @Transactional(readOnly = true)
    public boolean isRateLimited(User user, String ip) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);

        long userFailures = mfaAttemptRepository.countFailedAttemptsByUserSince(user, windowStart);
        if (userFailures >= MAX_FAILED_ATTEMPTS) {
            log.warn("MFA brute-force locked for user {}: {} failed attempts in last {}m",
                    user.getUserId(), userFailures, WINDOW_MINUTES);
            return true;
        }

        if (ip != null) {
            long ipFailures = mfaAttemptRepository.countFailedAttemptsByIpSince(ip, windowStart);
            if (ipFailures >= MAX_FAILED_ATTEMPTS) {
                log.warn("MFA IP-based throttling activated after {} failed attempts in {}m",
                        ipFailures, WINDOW_MINUTES);
                return true;
            }
        }

        return false;
    }

    @Transactional
    public void recordAttempt(User user, boolean success, String ip) {
        MfaAttempt attempt = MfaAttempt.builder()
                .user(user)
                .success(success)
                .ip(ip)
                .build();
        mfaAttemptRepository.save(attempt);
    }
}
