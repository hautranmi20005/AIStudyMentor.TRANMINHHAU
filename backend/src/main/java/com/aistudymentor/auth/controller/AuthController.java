package com.aistudymentor.auth.controller;

import com.aistudymentor.auth.dto.LoginRequest;
import com.aistudymentor.auth.dto.LoginResponse;
import com.aistudymentor.auth.dto.LogoutRequest;
import com.aistudymentor.auth.dto.MfaVerifyRequest;
import com.aistudymentor.auth.dto.MfaVerifyResponse;
import com.aistudymentor.auth.dto.RefreshTokenRequest;
import com.aistudymentor.auth.dto.RegisterRequest;
import com.aistudymentor.auth.dto.RegisterResponse;
import com.aistudymentor.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Public — registers a new user (triggers guardian consent email if minor, PD-002).
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login
     * Public — returns MFA_SESSION token if MFA enrolled, or full tokens if not yet enrolled.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        LoginResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/auth/mfa/verify
     * Public — verifies TOTP code using an MFA_SESSION token; issues full access + refresh tokens.
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        MfaVerifyResponse response = authService.verifyMfa(request, clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/auth/refresh
     * Public — rotates refresh token (PD-003: Redis blocklist).
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/auth/logout
     * Protected — revokes the supplied refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        String token = (request != null) ? request.getRefreshToken() : null;
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────── HELPER ───────────────────────────

    private String resolveClientIp(HttpServletRequest request) {
        // Only trust the socket peer; caller-supplied forwarding headers are spoofable.
        return request.getRemoteAddr();
    }
}
