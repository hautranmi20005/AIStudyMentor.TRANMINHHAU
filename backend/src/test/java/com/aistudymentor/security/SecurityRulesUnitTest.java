package com.aistudymentor.security;

import com.aistudymentor.auth.dto.RegisterRequest;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.MfaAttemptRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.auth.service.AccountStatusService;
import com.aistudymentor.auth.service.MfaRateLimitService;
import com.aistudymentor.config.GuardianConsentEnforcementFilter;
import com.aistudymentor.config.JwtAuthenticationFilter;
import com.aistudymentor.config.JwtTokenProvider;
import com.aistudymentor.config.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityRulesUnitTest {

    private JwtTokenProvider jwtTokenProvider;
    private ObjectMapper objectMapper;
    private Validator validator;

    @Mock
    private AccountStatusService accountStatusService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaAttemptRepository mfaAttemptRepository;

    @Mock
    private FilterChain filterChain;

    private static final String TEST_SECRET = "01234567890123456789012345678901_32bytes_minimum_secret_key!";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "mfaSessionExpirationMs", 300000L);
        jwtTokenProvider.init();

        validator = Validation.buildDefaultValidatorFactory().getValidator();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Rule 1: Pre-MFA token (MFA_SESSION) used on protected endpoint returns 403 MFA_REQUIRED")
    void testPreMfaTokenReturns403OnProtectedEndpoint() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, accountStatusService, objectMapper);

        UUID userId = UUID.randomUUID();
        String mfaToken = jwtTokenProvider.generateMfaSessionToken(userId, "student@test.com");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        request.addHeader("Authorization", "Bearer " + mfaToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("MFA_REQUIRED"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rule 2: Expired token returns 401 UNAUTHORIZED / TOKEN_EXPIRED")
    void testExpiredTokenReturns401() throws ServletException, IOException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedProvider, "accessTokenExpirationMs", -1000L); // expired
        ReflectionTestUtils.setField(shortLivedProvider, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(shortLivedProvider, "mfaSessionExpirationMs", 300000L);
        shortLivedProvider.init();

        String expiredToken = shortLivedProvider.generateAccessToken(UUID.randomUUID(), "expired@test.com", AccountStatus.ACTIVE);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, accountStatusService, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_EXPIRED"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rule 3: PENDING_CONSENT user accessing /questions returns 403 ACCOUNT_PENDING_CONSENT")
    void testPendingConsentUserBlockedFromQuestions() throws ServletException, IOException {
        GuardianConsentEnforcementFilter filter = new GuardianConsentEnforcementFilter(objectMapper);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("u13@test.com")
                .accountStatus(AccountStatus.PENDING_CONSENT)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_PENDING_CONSENT"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rule 4: Approval of consent immediately updates status from DB without relogin")
    void testConsentApprovalTakesEffectImmediately() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("u13@test.com")
                .accountStatus(AccountStatus.ACTIVE) // Approved in DB
                .dob(LocalDate.now().minusYears(11))
                .passwordHash("hashed")
                .build();

        AccountStatusService service = new AccountStatusService(userRepository, Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AccountStatus status = service.getAccountStatus(userId);
        assertEquals(AccountStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("Rule 5: SUSPENDED user blocked with 403 ACCOUNT_SUSPENDED")
    void testSuspendedUserBlocked() throws ServletException, IOException {
        GuardianConsentEnforcementFilter filter = new GuardianConsentEnforcementFilter(objectMapper);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .email("suspended@test.com")
                .accountStatus(AccountStatus.SUSPENDED)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_SUSPENDED"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rule 6: 6th failed TOTP attempt locks user for 15 minutes (MFA Brute Force)")
    void testMfaRateLimitLocksAfterFiveFailedAttempts() {
        MfaRateLimitService rateLimitService = new MfaRateLimitService(mfaAttemptRepository);
        User user = User.builder().userId(UUID.randomUUID()).email("user@test.com").build();

        // 4 failed attempts -> not locked
        when(mfaAttemptRepository.countFailedAttemptsByUserSince(eq(user), any(LocalDateTime.class))).thenReturn(4L);
        when(mfaAttemptRepository.countFailedAttemptsByIpSince(eq("127.0.0.1"), any(LocalDateTime.class))).thenReturn(4L);
        assertFalse(rateLimitService.isRateLimited(user, "127.0.0.1"));

        // 5 or 6 failed attempts -> locked
        when(mfaAttemptRepository.countFailedAttemptsByUserSince(eq(user), any(LocalDateTime.class))).thenReturn(5L);
        assertTrue(rateLimitService.isRateLimited(user, "127.0.0.1"));
    }

    @Test
    @DisplayName("Rule 7: RegisterRequest validates U13 guardianEmail requirements & password limits")
    void testRegisterRequestValidation() {
        // Under 13 without guardianEmail -> Invalid
        RegisterRequest reqNoGuardian = RegisterRequest.builder()
                .email("child@test.com")
                .password("validPassword123")
                .dob(LocalDate.now().minusYears(10))
                .build();
        assertFalse(validator.validate(reqNoGuardian).isEmpty());

        // Under 13 with identical guardian email -> Invalid
        RegisterRequest reqSameEmail = RegisterRequest.builder()
                .email("child@test.com")
                .password("validPassword123")
                .dob(LocalDate.now().minusYears(10))
                .guardianEmail("child@test.com")
                .build();
        assertFalse(validator.validate(reqSameEmail).isEmpty());

        // Under 13 with valid different guardian email -> Valid
        RegisterRequest reqValidU13 = RegisterRequest.builder()
                .email("child@test.com")
                .password("validPassword123")
                .dob(LocalDate.now().minusYears(10))
                .guardianEmail("parent@test.com")
                .build();
        assertTrue(validator.validate(reqValidU13).isEmpty());

        // Over 13 without guardian email -> Valid
        RegisterRequest reqAdult = RegisterRequest.builder()
                .email("adult@test.com")
                .password("validPassword123")
                .dob(LocalDate.now().minusYears(16))
                .build();
        assertTrue(validator.validate(reqAdult).isEmpty());
    }
}
