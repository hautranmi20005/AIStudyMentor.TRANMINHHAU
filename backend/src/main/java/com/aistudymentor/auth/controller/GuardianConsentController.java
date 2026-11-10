package com.aistudymentor.auth.controller;

import com.aistudymentor.auth.dto.GuardianConsentResponse;
import com.aistudymentor.auth.service.GuardianConsentService;
import com.aistudymentor.config.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guardian-consent")
@RequiredArgsConstructor
public class GuardianConsentController {

    private final GuardianConsentService guardianConsentService;

    /**
     * GET /api/v1/guardian-consent/verify?token=...&action=approve|reject
     * PUBLIC — renders an inert confirmation page; link scanners cannot consume the token.
     */
    @GetMapping("/verify")
    @SuppressWarnings("null")
    public ResponseEntity<String> confirm(
            @RequestParam("token") String token,
            @RequestParam("action") String action) {
        String confirmationPage = guardianConsentService.renderConsentConfirmation(token, action);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .body(confirmationPage);
    }

    /**
     * POST /api/v1/guardian-consent/verify
     * PUBLIC — changes account status only after the guardian confirms the action.
     */
    @PostMapping("/verify")
    public ResponseEntity<GuardianConsentResponse> verify(
            @RequestParam("token") String token,
            @RequestParam("action") String action,
            HttpServletRequest request) {
        String ip = resolveClientIp(request);
        GuardianConsentResponse response = guardianConsentService.verifyConsent(token, action, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/guardian-consent/status
     * PROTECTED — student checks their own consent status.
     */
    @GetMapping("/status")
    public ResponseEntity<GuardianConsentResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        GuardianConsentResponse response = guardianConsentService.getConsentStatus(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/guardian-consent/resend
     * PROTECTED — student requests re-sending of the guardian consent email.
     */
    @PostMapping("/resend")
    public ResponseEntity<Void> resend(@AuthenticationPrincipal UserPrincipal principal) {
        guardianConsentService.resendConsentEmail(principal.getUserId());
        return ResponseEntity.accepted().build();
    }

    // ─────────────────────────── HELPER ───────────────────────────

    private String resolveClientIp(HttpServletRequest request) {
        // Do not trust caller-controlled X-Forwarded-For unless a trusted proxy layer is configured.
        return request.getRemoteAddr();
    }
}
