package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuardianConsentEnforcementFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            AccountStatus status = userPrincipal.getAccountStatus();
            String requestUri = request.getRequestURI();

            // 1. Enforce SUSPENDED account policy
            if (status == AccountStatus.SUSPENDED) {
                log.warn("Blocked SUSPENDED user {} from accessing {}", userPrincipal.getUserId(), requestUri);
                sendErrorResponse(response, requestUri, HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN", "ACCOUNT_SUSPENDED", "Your account has been suspended. Please contact support.");
                return;
            }

            // 2. Enforce PENDING_CONSENT (Under-13) policy
            if (status == AccountStatus.PENDING_CONSENT) {
                // H5 Whitelist: Auth, GET/POST consent status, read/update profile, error, actuator
                boolean isWhitelisted = requestUri.startsWith("/api/v1/auth/")
                        || requestUri.startsWith("/api/v1/users/me/guardian-consent")
                    || requestUri.equals("/api/v1/guardian-consent/status")
                    || requestUri.equals("/api/v1/guardian-consent/resend")
                        || requestUri.startsWith("/api/v1/users/me/profile")
                        || requestUri.startsWith("/error")
                        || requestUri.startsWith("/actuator/");

                if (!isWhitelisted) {
                    log.warn("Blocked U13 user {} from accessing {} due to PENDING_CONSENT",
                            userPrincipal.getUserId(), requestUri);
                    sendErrorResponse(response, requestUri, HttpServletResponse.SC_FORBIDDEN,
                            "FORBIDDEN", "ACCOUNT_PENDING_CONSENT",
                            "Guardian consent is required before accessing study features. Please check your parent/guardian email for verification.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String path, int status,
                                   String error, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
