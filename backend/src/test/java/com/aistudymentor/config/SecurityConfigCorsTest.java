package com.aistudymentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void sameOriginGuardianConsentFormPostDoesNotTriggerCorsRejection() throws Exception {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/guardian-consent/verify");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        request.addHeader("Origin", "http://127.0.0.1:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        new CorsFilter(source).doFilter(request, response,
                (servletRequest, servletResponse) -> continued.set(true));

        assertTrue(continued.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void mailpitAndSandboxOriginsAreAllowedForGuardianConsentGetAndPost() throws Exception {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        List<String> origins = List.of(
                "http://localhost:8025",
                "http://127.0.0.1:8025",
                "null");

        for (String origin : origins) {
            for (String method : List.of("GET", "POST")) {
                MockHttpServletRequest request = new MockHttpServletRequest(
                        method, "/api/v1/guardian-consent/verify");
                request.setScheme("http");
                request.setServerName("localhost");
                request.setServerPort(8080);
                request.addHeader("Origin", origin);
                MockHttpServletResponse response = new MockHttpServletResponse();
                AtomicBoolean continued = new AtomicBoolean();

                new CorsFilter(source).doFilter(request, response,
                        (servletRequest, servletResponse) -> {
                            continued.set(true);
                            ((MockHttpServletResponse) servletResponse).getWriter().write(
                                    "<h1>Consent Approved Successfully</h1>");
                        });

                assertTrue(continued.get(), method + " from " + origin + " should reach the consent endpoint");
                assertEquals(200, response.getStatus());
                assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
                assertTrue(response.getContentAsString().contains("Consent Approved Successfully"));
            }
        }
    }

    @Test
    void mailpitOriginRemainsRejectedForOtherApiEndpoints() throws Exception {
        SecurityConfig securityConfig = securityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        for (String origin : List.of("http://127.0.0.1:8025", "null")) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/questions");
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.addHeader("Origin", origin);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean continued = new AtomicBoolean();

            new CorsFilter(source).doFilter(request, response,
                    (servletRequest, servletResponse) -> continued.set(true));

            assertFalse(continued.get(), origin + " must not be allowed on other API endpoints");
            assertEquals(403, response.getStatus());
            assertEquals("Invalid CORS request", response.getContentAsString());
        }
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
