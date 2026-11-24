package com.aistudymentor.auth.controller;

import com.aistudymentor.auth.dto.GuardianConsentResponse;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.service.GuardianConsentService;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.config.ApiRateLimitFilter;
import com.aistudymentor.config.CustomAccessDeniedHandler;
import com.aistudymentor.config.GuardianConsentEnforcementFilter;
import com.aistudymentor.config.JwtAuthenticationFilter;
import com.aistudymentor.config.RestAuthenticationEntryPoint;
import com.aistudymentor.config.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GuardianConsentControllerTest {

    @Test
    void approvalPostReturnsSuccessHtml() {
        GuardianConsentService service = mock(GuardianConsentService.class);
        GuardianConsentController controller = new GuardianConsentController(service);
        HttpServletRequest request = new MockHttpServletRequest();
        GuardianConsentResponse approval = GuardianConsentResponse.builder()
                .status(ConsentStatus.APPROVED)
                .canAccessCoreFeatures(true)
                .build();
        String successPage = "<h1>Consent Approved Successfully</h1>";
        when(service.verifyConsent("one-time-token", "approve", "127.0.0.1")).thenReturn(approval);
        when(service.renderConsentResult(true)).thenReturn(successPage);

        ResponseEntity<String> response = controller.verify("one-time-token", "approve", request);

        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(response.getBody().contains("Consent Approved Successfully"));
        verify(service).renderConsentResult(true);
    }

    @Test
    void emailLinkGetRendersAutoSubmittingConfirmationPage() throws Exception {
        GuardianConsentService service = mock(GuardianConsentService.class);
        when(service.renderConsentConfirmation("one-time-token", "approve"))
                .thenReturn("<form method=\"post\"><input name=\"token\" value=\"one-time-token\"></form>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GuardianConsentController(service)).build();

        mvc.perform(get("/api/v1/guardian-consent/verify")
                        .queryParam("token", "one-time-token")
                        .queryParam("action", "approve"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("method=\"post\"")));

        verify(service).renderConsentConfirmation("one-time-token", "approve");
    }

    @Test
    void formPostAcceptsTokenAndActionAndReturnsConfirmationHtml() throws Exception {
        GuardianConsentService service = mock(GuardianConsentService.class);
        when(service.verifyConsent("one-time-token", "approve", "127.0.0.1"))
                .thenReturn(GuardianConsentResponse.builder().status(ConsentStatus.APPROVED).build());
        when(service.renderConsentResult(true)).thenReturn("<h1>Consent Approved Successfully</h1>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GuardianConsentController(service)).build();

        mvc.perform(post("/api/v1/guardian-consent/verify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "one-time-token")
                        .param("action", "approve"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Consent Approved Successfully")));

        verify(service).verifyConsent("one-time-token", "approve", "127.0.0.1");
    }

    @Test
    void expiredTokenReturnsClearHtmlError() throws Exception {
        GuardianConsentService service = mock(GuardianConsentService.class);
        doThrow(new BusinessException(HttpStatus.GONE, "CONSENT_TOKEN_EXPIRED", "private token detail"))
                .when(service).verifyConsent("expired-token", "approve", "127.0.0.1");
        when(service.renderConsentError("CONSENT_TOKEN_EXPIRED"))
                .thenReturn("<h1>Unable to process consent</h1><p>This consent link has expired.</p>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GuardianConsentController(service)).build();

        mvc.perform(post("/api/v1/guardian-consent/verify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "expired-token")
                        .param("action", "approve"))
                .andExpect(status().isGone())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("This consent link has expired.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("private token detail"))));
    }

    @Test
    void missingDecisionParametersReturnHtmlFeedback() throws Exception {
        GuardianConsentService service = mock(GuardianConsentService.class);
        when(service.renderConsentError("INVALID_CONSENT_ACTION"))
                .thenReturn("<h1>Unable to process consent</h1><p>This link is incomplete.</p>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GuardianConsentController(service)).build();

        mvc.perform(post("/api/v1/guardian-consent/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("This link is incomplete.")));

        verify(service, never()).verifyConsent(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mailpitAndSandboxOriginsCanPostConsentAndReceiveHtmlConfirmation() throws Exception {
        GuardianConsentService service = mock(GuardianConsentService.class);
        when(service.verifyConsent("one-time-token", "approve", "127.0.0.1"))
                .thenReturn(GuardianConsentResponse.builder().status(ConsentStatus.APPROVED).build());
        when(service.renderConsentResult(true)).thenReturn("<h1>Consent Approved Successfully</h1>");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GuardianConsentController(service))
                .addFilters(new CorsFilter(consentCorsConfigurationSource()))
                .build();

        for (String origin : new String[] {
                "http://localhost:8025", "http://127.0.0.1:8025", "null"}) {
            mvc.perform(post("/api/v1/guardian-consent/verify")
                            .header("Origin", origin)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("token", "one-time-token")
                            .param("action", "approve"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString("Consent Approved Successfully")))
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }

        verify(service, times(3)).verifyConsent("one-time-token", "approve", "127.0.0.1");
    }

    private CorsConfigurationSource consentCorsConfigurationSource() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(ApiRateLimitFilter.class),
                mock(GuardianConsentEnforcementFilter.class),
                mock(RestAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class));
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        return securityConfig.corsConfigurationSource();
    }
}
