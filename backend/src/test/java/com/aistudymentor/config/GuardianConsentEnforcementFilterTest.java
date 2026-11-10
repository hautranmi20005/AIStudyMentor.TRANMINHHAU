package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardianConsentEnforcementFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pendingConsentBlocksStudyRoutesWithRequestedCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        authenticatePendingStudent();

        new GuardianConsentEnforcementFilter(new ObjectMapper().findAndRegisterModules()).doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> continued.set(true));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_PENDING_CONSENT"));
        assertFalse(continued.get());
    }

    @Test
    void pendingConsentCanReadStatusAndRequestResend() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/guardian-consent/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        authenticatePendingStudent();

        new GuardianConsentEnforcementFilter(new ObjectMapper().findAndRegisterModules()).doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> continued.set(true));

        assertTrue(continued.get());
        assertEquals(200, response.getStatus());
    }

    private void authenticatePendingStudent() {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("student@example.test")
                .accountStatus(AccountStatus.PENDING_CONSENT)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}