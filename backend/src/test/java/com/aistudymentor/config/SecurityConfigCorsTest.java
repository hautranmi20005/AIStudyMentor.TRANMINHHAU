package com.aistudymentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SuppressWarnings("null")
class SecurityConfigCorsTest {

    @Test
    void corsOnlyAllowsConfiguredOriginsAndExplicitHeadersWithoutCredentials() {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins",
                "https://study.example, https://admin.example");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration cors = java.util.Objects.requireNonNull(source.getCorsConfiguration(
            new MockHttpServletRequest("GET", "/api/v1/questions")));

        assertEquals(List.of("https://study.example", "https://admin.example"), cors.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), cors.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type", "Accept", "Origin"), cors.getAllowedHeaders());
        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertNull(cors.checkOrigin("https://attacker.example"));
        assertEquals("https://study.example", cors.checkOrigin("https://study.example"));
    }

    @Test
    void corsRejectsAllOriginsWhenAllowListIsUnconfigured() {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        CorsConfiguration cors = java.util.Objects.requireNonNull(
            securityConfig.corsConfigurationSource().getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/questions")));

        assertTrue(cors.getAllowedOrigins().isEmpty());
        assertNull(cors.checkOrigin("https://any.example"));
    }

    @Test
    void corsConfigurationRejectsWildcardAndPaths() {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "*");
        assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);

        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "https://study.example/path");
        assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);
    }

    private SecurityConfig securityConfig() {
        return new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(ApiRateLimitFilter.class),
                mock(GuardianConsentEnforcementFilter.class),
                mock(RestAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class));
    }
}
