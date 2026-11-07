package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
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
import java.util.HashMap;
import java.util.Map;

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
            if (userPrincipal.getAccountStatus() == AccountStatus.PENDING_CONSENT) {
                String requestUri = request.getRequestURI();

                // Whitelisted endpoints for U13 pending consent accounts:
                // 1. Consent status check endpoint
                // 2. Auth endpoints
                // 3. Error endpoint
                boolean isWhitelisted = requestUri.startsWith("/api/v1/users/me/guardian-consent")
                        || requestUri.startsWith("/api/v1/auth/")
                        || requestUri.startsWith("/error");

                if (!isWhitelisted) {
                    log.warn("Blocked U13 user {} from accessing {} due to PENDING_CONSENT",
                            userPrincipal.getUserId(), requestUri);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put("status", HttpServletResponse.SC_FORBIDDEN);
                    errorBody.put("error", "FORBIDDEN");
                    errorBody.put("code", "GUARDIAN_CONSENT_REQUIRED");
                    errorBody.put("message", "Guardian consent is required before accessing study features. Please check your parent/guardian email for verification.");

                    objectMapper.writeValue(response.getOutputStream(), errorBody);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
