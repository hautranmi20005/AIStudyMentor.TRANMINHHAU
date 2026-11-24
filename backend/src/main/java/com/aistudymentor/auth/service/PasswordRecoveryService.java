package com.aistudymentor.auth.service;

import com.aistudymentor.auth.dto.PasswordRecoveryRequest;
import com.aistudymentor.auth.dto.PasswordResetRequest;
import com.aistudymentor.auth.entity.PasswordResetToken;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.PasswordResetTokenRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PasswordRecoveryService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long TOKEN_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.mail.from:no-reply@localhost}")
    private String fromAddress;

    @Transactional
    public void requestReset(PasswordRecoveryRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> sendReset(user));
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        validatePassword(request.getNewPassword());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHashForUpdate(sha256Hex(request.getToken()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                        "The password reset link is invalid or expired."));
        if (!LocalDateTime.now().isBefore(resetToken.getExpiresAt())) {
            resetTokenRepository.delete(resetToken);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                    "The password reset link is invalid or expired.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);
        resetTokenRepository.deleteByUser_UserId(user.getUserId());
    }

    private void sendReset(User user) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES);
        resetTokenRepository.deleteByUser_UserId(user.getUserId());
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(expiresAt)
                .build());

        String resetUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build().toUriString();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your AI Study Mentor password");
            helper.setText("Use this one-time link within 60 minutes to reset your password: "
                    + resetUrl, false);
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            log.error("Password recovery email could not be sent ({})", exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_UNAVAILABLE",
                    "Password recovery email is temporarily unavailable.");
        }
    }

    private void validatePassword(String password) {
        boolean strong = password.length() >= 12
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
        if (!strong) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD",
                    "Password must be at least 12 characters and include upper-case, lower-case, and numeric characters.");
        }
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
