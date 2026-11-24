package com.aistudymentor.auth.controller;

import com.aistudymentor.auth.dto.GuardianConsentResponse;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.service.GuardianConsentService;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.config.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
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
     * PUBLIC — renders a page that submits the decision with POST; GET itself never changes consent.
     */
    @GetMapping("/verify")
    @SuppressWarnings("null")
    public ResponseEntity<String> confirm(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "action", required = false) String action) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(action)) {
            String errorCode = StringUtils.hasText(action)
                    ? "CONSENT_TOKEN_NOT_FOUND" : "INVALID_CONSENT_ACTION";
            return htmlResponse(HttpStatus.BAD_REQUEST, guardianConsentService.renderConsentError(errorCode));
        }
        try {
            return htmlResponse(guardianConsentService.renderConsentConfirmation(token, action));
        } catch (BusinessException exception) {
            return htmlResponse(exception.getStatus(),
                    guardianConsentService.renderConsentError(exception.getCode()));
        }
    }

    /**
     * POST /api/v1/guardian-consent/verify
     * PUBLIC — applies the decision submitted by the consent link's confirmation page.
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verify(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "action", required = false) String action,
            HttpServletRequest request) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(action)) {
            String errorCode = StringUtils.hasText(action)
                    ? "CONSENT_TOKEN_NOT_FOUND" : "INVALID_CONSENT_ACTION";
            return htmlResponse(HttpStatus.BAD_REQUEST, guardianConsentService.renderConsentError(errorCode));
        }
        try {
            String ip = resolveClientIp(request);
            GuardianConsentResponse response = guardianConsentService.verifyConsent(token, action, ip);
            String confirmationPage = guardianConsentService.renderConsentResult(
                    response.getStatus() == ConsentStatus.APPROVED);
            return htmlResponse(confirmationPage);
        } catch (BusinessException exception) {
            return htmlResponse(exception.getStatus(),
                    guardianConsentService.renderConsentError(exception.getCode()));
        }
    }

    private ResponseEntity<String> htmlResponse(String content) {
        return htmlResponse(HttpStatus.OK, content);
    }

    private ResponseEntity<String> htmlResponse(HttpStatus status, String content) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy",
                        "default-src 'none'; form-action 'self'; script-src 'unsafe-inline'; "
                                + "style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'")
                .body(content);
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
