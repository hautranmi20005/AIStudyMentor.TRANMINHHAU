package com.aistudymentor.auth.service;

import com.aistudymentor.auth.dto.GuardianConsentResponse;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.entity.GuardianConsent;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.GuardianConsentRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.config.GuardianConsentEnforcementFilter;
import com.aistudymentor.config.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class GuardianConsentServiceTest {

    private GuardianConsentRepository consentRepository;
    private UserRepository userRepository;
    private AccountStatusService accountStatusService;
    private JavaMailSender mailSender;
    private GuardianConsentService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        consentRepository = mock(GuardianConsentRepository.class);
        userRepository = mock(UserRepository.class);
        accountStatusService = mock(AccountStatusService.class);
        mailSender = mock(JavaMailSender.class);
        service = new GuardianConsentService(consentRepository, userRepository, accountStatusService, mailSender);
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.example.test/");
        ReflectionTestUtils.setField(service, "fromAddress", "consent@example.test");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void initiationStoresOnlyHashedTokenAndSendsApproveAndRejectLinks() throws Exception {
        User user = user();
        when(consentRepository.save(any(GuardianConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.initiateConsentFlow(user, "guardian@example.test");

        var consentCaptor = org.mockito.ArgumentCaptor.forClass(GuardianConsent.class);
        verify(consentRepository).save(consentCaptor.capture());
        GuardianConsent saved = consentCaptor.getValue();
        assertEquals(ConsentStatus.PENDING, saved.getStatus());
        assertEquals(64, saved.getTokenHash().length());
        assertTrue(saved.getTokenExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(0, saved.getResendCount());

        MimeMessage message = sentMessage();
        String html = alternativeHtml(message);
        assertTrue(html.contains("Approve consent"));
        assertTrue(html.contains("Reject consent"));
        assertTrue(html.contains("action=approve"));
        assertTrue(html.contains("action=reject"));
        assertTrue(html.contains("https://api.example.test/api/v1/guardian-consent/verify?token="));
        assertFalse(html.contains("10.0.2.2"));
        String rawToken = html.replaceFirst("(?s).*?token=([A-Za-z0-9_-]+)&amp;action=approve.*", "$1");
        assertNotEquals(rawToken, saved.getTokenHash());
        assertEquals(hash(rawToken), saved.getTokenHash());
    }

    @Test
    void verificationConsumesTokenAndActivatesUserOnlyAfterApproval() throws Exception {
        String rawToken = "one-time-token";
        User user = user();
        user.setAccountStatus(AccountStatus.PENDING_CONSENT);
        GuardianConsent consent = consent(user, hash(rawToken));
        when(consentRepository.findByTokenHashForUpdate(hash(rawToken))).thenReturn(java.util.Optional.of(consent));

        GuardianConsentResponse response = service.verifyConsent(rawToken, "approve", "192.0.2.10");

        assertEquals(ConsentStatus.APPROVED, response.getStatus());
        assertTrue(response.isCanAccessCoreFeatures());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertNull(consent.getTokenHash());
        verify(userRepository).save(user);
        verify(accountStatusService).invalidateCache(user.getUserId());
    }

    @Test
    void approvedMinorCanAccessStudyRoutesAfterConsentVerification() throws Exception {
        String rawToken = "one-time-token";
        User user = user();
        GuardianConsent consent = consent(user, hash(rawToken));
        when(consentRepository.findByTokenHashForUpdate(hash(rawToken))).thenReturn(java.util.Optional.of(consent));

        service.verifyConsent(rawToken, "approve", "192.0.2.10");

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        UserPrincipal principal = UserPrincipal.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        new GuardianConsentEnforcementFilter(new ObjectMapper().findAndRegisterModules()).doFilter(
                request, response, (servletRequest, servletResponse) -> continued.set(true));

        assertTrue(continued.get());
        assertEquals(200, response.getStatus());
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsUnknownActionBeforeLookingUpToken() {
        assertThrows(BusinessException.class, () -> service.verifyConsent("token", "anything", "127.0.0.1"));
        verify(consentRepository, never()).findByTokenHashForUpdate(anyString());
    }

    @Test
    void emailLinkConfirmationPageDoesNotConsumeToken() {
        String page = service.renderConsentConfirmation("one-time-token", "approve");

        assertTrue(page.contains("method=\"post\""));
        assertTrue(page.contains("document.getElementById(\"consent-form\").submit()"));
        assertTrue(page.contains("name=\"token\" value=\"one-time-token\""));
        assertTrue(page.contains("name=\"action\" value=\"approve\""));
        verify(consentRepository, never()).findByTokenHashForUpdate(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approvalResultPageShowsSuccessMessage() {
        String page = service.renderConsentResult(true);

        assertTrue(page.contains("<title>Consent Approved Successfully</title>"));
        assertTrue(page.contains("Consent Approved Successfully"));
        assertTrue(page.contains("The student account can now access its study features."));
    }

    @Test
    void expiredConsentResultUsesClearUserFacingMessage() {
        String page = service.renderConsentError("CONSENT_TOKEN_EXPIRED");

        assertTrue(page.contains("<title>Unable to process consent</title>"));
        assertTrue(page.contains("This consent link has expired."));
        assertFalse(page.contains("CONSENT_TOKEN_EXPIRED"));
    }

    @Test
    void scheduledReminderRotatesTokenWithoutExtendingInitialExpiry() {
        User user = user();
        GuardianConsent consent = consent(user, "old-hash");
        LocalDateTime expiry = LocalDateTime.now().plusHours(30);
        consent.setTokenExpiresAt(expiry);
        consent.setLastSentAt(LocalDateTime.now().minusHours(25));
        consent.setResendCount(0);
        when(consentRepository.findPendingReminders(
                eq(ConsentStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(consent));
        when(consentRepository.save(any(GuardianConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendDueConsentReminders();

        assertEquals(1, consent.getResendCount());
        assertEquals(expiry, consent.getTokenExpiresAt());
        assertNotNull(consent.getTokenHash());
        assertNotEquals("old-hash", consent.getTokenHash());
        verify(mailSender).send(any(MimeMessage.class));
        verify(consentRepository).findPendingReminders(
                eq(ConsentStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class), eq(3));
    }

    private User user() {
        return User.builder()
                .userId(UUID.randomUUID())
                .email("student@example.test")
                .accountStatus(AccountStatus.PENDING_CONSENT)
                .build();
    }

    private GuardianConsent consent(User user, String tokenHash) {
        return GuardianConsent.builder()
                .consentId(UUID.randomUUID())
                .user(user)
                .guardianEmail("guardian@example.test")
                .status(ConsentStatus.PENDING)
                .tokenHash(tokenHash)
                .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                .resendCount(0)
                .lastSentAt(LocalDateTime.now())
                .build();
    }

    private MimeMessage sentMessage() throws Exception {
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();
        return message;
    }

    private String alternativeHtml(MimeMessage message) throws Exception {
        String html = findHtmlPart(message);
        assertNotNull(html);
        return html;
    }

    private String findHtmlPart(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return part.getContent().toString();
        }
        Object content = part.getContent();
        if (content instanceof jakarta.mail.Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String html = findHtmlPart(multipart.getBodyPart(index));
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}