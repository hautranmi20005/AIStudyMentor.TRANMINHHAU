package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.service.AccountStatusService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccountStatusService accountStatusService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = extractJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            // Check for expired token explicitly (Decision D5 -> 401)
            if (jwtTokenProvider.isTokenExpired(jwt)) {
                sendErrorResponse(response, request.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED", "TOKEN_EXPIRED", "The provided authentication token has expired.");
                return;
            }

            if (jwtTokenProvider.validateToken(jwt)) {
                String tokenType = jwtTokenProvider.getTokenType(jwt);

                // Decision D5: MFA_SESSION token used on protected endpoints returns 403 MFA_REQUIRED
                if ("MFA_SESSION".equals(tokenType)) {
                    String requestUri = request.getRequestURI();
                    if (!requestUri.equals("/api/v1/auth/mfa/verify")) {
                        sendErrorResponse(response, requestUri, HttpServletResponse.SC_FORBIDDEN,
                                "FORBIDDEN", "MFA_REQUIRED", "Multi-factor authentication must be completed before accessing this resource.");
                        return;
                    }
                } else if ("ACCESS".equals(tokenType)) {
                    try {
                        String email = jwtTokenProvider.getEmailFromToken(jwt);
                        UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);

                        // H1: Fetch live status from DB/Cache (never blindly trusting token claim)
                        AccountStatus liveStatus = accountStatusService.getAccountStatus(userId);

                        UserPrincipal userPrincipal = UserPrincipal.builder()
                                .userId(userId)
                                .email(email)
                                .accountStatus(liveStatus)
                                .build();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userPrincipal,
                                        null,
                                        userPrincipal.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception ex) {
                        log.warn("Failed to authenticate request token ({})", ex.getClass().getSimpleName());
                    }
                }
            } else {
                // Invalid token syntax or signature -> 401
                sendErrorResponse(response, request.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED", "INVALID_TOKEN", "The provided authentication token is invalid.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
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
