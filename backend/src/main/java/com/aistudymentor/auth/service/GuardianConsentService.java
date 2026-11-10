package com.aistudymentor.auth.service;

import com.aistudymentor.auth.dto.GuardianConsentResponse;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.entity.GuardianConsent;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.GuardianConsentRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Guardian Consent Service (PD-002).
 * Implements the email loop with SHA-256 token hash and 72h expiration.
 * Legal review pending — not production-ready.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // Spring Data and mail APIs use nullability annotations not inferred by the IDE.
public class GuardianConsentService {

    private static final long TOKEN_EXPIRY_HOURS = 72;
    private static final int MAX_RESEND = 3;
    private static final long RESEND_COOLDOWN_MINUTES = 5;
    private static final long REMINDER_INTERVAL_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CONSENT_VERIFY_PATH = "/api/v1/guardian-consent/verify";

    private final GuardianConsentRepository guardianConsentRepository;
    private final UserRepository userRepository;
    private final AccountStatusService accountStatusService;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.mail.from:no-reply@localhost}")
    private String fromAddress;

    // ─────────────────────────── INITIATE CONSENT ───────────────────────────

    /**
     * Called from AuthService.register() — creates GuardianConsent record and sends email.
     */
    @Transactional
    public void initiateConsentFlow(User user, String guardianEmail) {
        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);
        LocalDateTime now = LocalDateTime.now();

        GuardianConsent consent = GuardianConsent.builder()
                .user(user)
                .guardianEmail(guardianEmail)
                .status(ConsentStatus.PENDING)
                .tokenHash(tokenHash)
            .tokenExpiresAt(now.plusHours(TOKEN_EXPIRY_HOURS))
                .resendCount(0)
            .lastSentAt(now)
                .build();
        guardianConsentRepository.save(consent);

        sendConsentEmail(guardianEmail, rawToken, user.getEmail(), consent.getTokenExpiresAt(), false);
        log.info("Guardian consent initiated for userId={}", user.getUserId());
    }

    // ─────────────────────────── VERIFY CONSENT ───────────────────────────

    /**
     * Renders an inert confirmation page for email links. A GET must never consume the
     * single-use token because mail security scanners routinely prefetch links.
     */
    public String renderConsentConfirmation(String rawToken, String action) {
        validateAction(action);
        try (InputStream templateStream = new ClassPathResource("templates/guardian-consent-confirm.html").getInputStream()) {
            return StreamUtils.copyToString(templateStream, StandardCharsets.UTF_8)
                    .replace("{{token}}", escapeHtml(rawToken))
                    .replace("{{action}}", escapeHtml(action.toLowerCase(java.util.Locale.ROOT)))
                    .replace("{{decision}}", "approve".equalsIgnoreCase(action) ? "approve" : "reject");
        } catch (IOException exception) {
            log.error("Guardian consent confirmation template is unavailable");
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "CONSENT_PAGE_UNAVAILABLE",
                    "The consent confirmation page is temporarily unavailable.");
        }
    }

    /**
     * Public POST endpoint submitted only after the guardian confirms the decision.
     */
    @Transactional
    public GuardianConsentResponse verifyConsent(String rawToken, String action, String ipAddress) {
        validateAction(action);
        String tokenHash = sha256Hex(rawToken);

        GuardianConsent consent = guardianConsentRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CONSENT_TOKEN_NOT_FOUND",
                        "Consent token not found or already used."));

        LocalDateTime now = LocalDateTime.now();
        if (consent.getTokenExpiresAt() == null || !now.isBefore(consent.getTokenExpiresAt())) {
            throw new BusinessException(HttpStatus.GONE, "CONSENT_TOKEN_EXPIRED",
                    "This consent link has expired. Please ask the student to request a new link.");
        }

        // Check already processed
        if (consent.getStatus() != ConsentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONSENT_ALREADY_PROCESSED",
                    "This consent has already been processed.");
        }

        boolean approved = "approve".equalsIgnoreCase(action);
        consent.setStatus(approved ? ConsentStatus.APPROVED : ConsentStatus.REJECTED);
        consent.setTokenHash(null); // consume token (one-time use)
        consent.setTokenExpiresAt(null);
        consent.setVerifiedAt(now);
        consent.setApprovedIp(ipAddress);
        guardianConsentRepository.save(consent);

        // Update user account status
        User user = consent.getUser();
        if (approved) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        } else {
            user.setAccountStatus(AccountStatus.SUSPENDED);
        }
        userRepository.save(user);
        accountStatusService.invalidateCache(user.getUserId());

        log.info("Guardian consent {} for userId={}", approved ? "APPROVED" : "REJECTED", user.getUserId());

        return GuardianConsentResponse.builder()
                .consentId(consent.getConsentId())
                .userId(user.getUserId())
                .guardianEmail(consent.getGuardianEmail())
                .status(consent.getStatus())
                .verifiedAt(consent.getVerifiedAt())
                .canAccessCoreFeatures(approved)
                .build();
    }

    // ─────────────────────────── RESEND ───────────────────────────

    @Transactional
    public void resendConsentEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));

        GuardianConsent consent = guardianConsentRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CONSENT_NOT_FOUND",
                        "No guardian consent record found for this user."));

        if (consent.getStatus() != ConsentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONSENT_ALREADY_PROCESSED",
                    "Consent has already been processed.");
        }

        if (consent.getResendCount() >= MAX_RESEND) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_LIMIT_REACHED",
                    "Maximum resend attempts reached.");
        }

        // Cooldown check
        if (consent.getLastSentAt() != null
                && consent.getLastSentAt().plusMinutes(RESEND_COOLDOWN_MINUTES).isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_TOO_SOON",
                    "Please wait before requesting another consent email.");
        }

        // Send first; only replace the stored hash after the provider accepts the email.
        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tokenExpiresAt = now.plusHours(TOKEN_EXPIRY_HOURS);
        sendConsentEmail(consent.getGuardianEmail(), rawToken, user.getEmail(), tokenExpiresAt, false);
        consent.setTokenHash(sha256Hex(rawToken));
        consent.setTokenExpiresAt(tokenExpiresAt);
        consent.setResendCount(consent.getResendCount() + 1);
        consent.setLastSentAt(now);
        guardianConsentRepository.save(consent);

        log.info("Consent email resent for userId={} (attempt #{})", userId, consent.getResendCount());
    }

    /**
     * Sends at most two automated reminders during the original 72-hour token window.
     * The repository locks due rows so multiple application instances do not send the
     * same reminder concurrently.
     */
    @Scheduled(fixedDelayString = "${app.guardian-consent.reminder-scan-delay-ms:3600000}")
    @Transactional
    public void sendDueConsentReminders() {
        LocalDateTime now = LocalDateTime.now();
        var dueConsents = guardianConsentRepository.findPendingReminders(
                ConsentStatus.PENDING,
                now.minusHours(REMINDER_INTERVAL_HOURS),
                now,
                MAX_RESEND);

        for (GuardianConsent consent : dueConsents) {
            String rawToken = generateRawToken();
            try {
                sendConsentEmail(consent.getGuardianEmail(), rawToken, consent.getUser().getEmail(),
                    consent.getTokenExpiresAt(), true);
                consent.setTokenHash(sha256Hex(rawToken));
                consent.setLastSentAt(now);
                consent.setResendCount(consent.getResendCount() + 1);
                guardianConsentRepository.save(consent);
                log.info("Guardian consent reminder sent for userId={}", consent.getUser().getUserId());
            } catch (BusinessException exception) {
                log.warn("Guardian consent reminder delivery failed for userId={}", consent.getUser().getUserId());
            }
        }
    }

    // ─────────────────────────── GET STATUS ───────────────────────────

    @Transactional(readOnly = true)
    public GuardianConsentResponse getConsentStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));

        GuardianConsent consent = guardianConsentRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CONSENT_NOT_FOUND",
                        "No guardian consent record found."));

        return GuardianConsentResponse.builder()
                .consentId(consent.getConsentId())
                .userId(userId)
                .guardianEmail(consent.getGuardianEmail())
                .status(consent.getStatus())
                .verifiedAt(consent.getVerifiedAt())
                .canAccessCoreFeatures(consent.getStatus() == ConsentStatus.APPROVED)
                .build();
    }

    // ─────────────────────────── HELPERS ───────────────────────────

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void sendConsentEmail(
            String guardianEmail,
            String rawToken,
            String studentEmail,
            LocalDateTime tokenExpiresAt,
            boolean reminder) {
        try {
            String approveUrl = buildConsentUrl(rawToken, "approve");
            String rejectUrl = buildConsentUrl(rawToken, "reject");
            String title = reminder ? "Reminder: guardian consent is still needed" : "Guardian consent required";
            long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), tokenExpiresAt).getSeconds();
            long hoursRemaining = Math.max(1, (secondsRemaining + 3599) / 3600);
            String htmlTemplate;
            try (InputStream templateStream = new ClassPathResource("templates/guardian-consent.html").getInputStream()) {
                htmlTemplate = StreamUtils.copyToString(templateStream, StandardCharsets.UTF_8);
            }
            String html = htmlTemplate
                    .replace("{{title}}", escapeHtml(title))
                    .replace("{{studentEmail}}", escapeHtml(studentEmail))
                    .replace("{{approveUrl}}", escapeHtml(approveUrl))
                    .replace("{{rejectUrl}}", escapeHtml(rejectUrl))
                        .replace("{{expiresHours}}", Long.toString(hoursRemaining));
            String plainText = title + "\n\nA student (" + studentEmail + ") requires your consent to use AI Study Mentor.\n\n"
                    + "Approve: " + approveUrl + "\nReject: " + rejectUrl + "\n\n"
                        + "The links expire within " + hoursRemaining + " hours.";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(guardianEmail);
            if (StringUtils.hasText(fromAddress)) {
                helper.setFrom(fromAddress);
            }
            helper.setSubject("[AI Study Mentor] " + title);
            helper.setText(plainText, html);
            mailSender.send(message);
        } catch (IOException | MessagingException | MailException exception) {
            log.error("Guardian consent email delivery failed ({})", exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CONSENT_EMAIL_DELIVERY_FAILED",
                    "Guardian consent email could not be delivered. Please try again later.");
        }
    }

    private String buildConsentUrl(String rawToken, String action) {
        String rootUrl = baseUrl.replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(rootUrl + CONSENT_VERIFY_PATH)
                .queryParam("token", rawToken)
                .queryParam("action", action)
                .build()
                .encode()
                .toUriString();
    }

    private void validateAction(String action) {
        if (!"approve".equalsIgnoreCase(action) && !"reject".equalsIgnoreCase(action)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONSENT_ACTION",
                    "Consent action must be approve or reject.");
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
