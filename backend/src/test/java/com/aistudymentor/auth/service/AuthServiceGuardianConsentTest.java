package com.aistudymentor.auth.service;

import com.aistudymentor.auth.dto.RegisterRequest;
import com.aistudymentor.auth.dto.RegisterResponse;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.repository.MfaFactorRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.common.util.AesGcmEncryptionUtil;
import com.aistudymentor.config.JwtTokenProvider;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class AuthServiceGuardianConsentTest {

    private UserRepository userRepository;
    private MfaFactorRepository mfaFactorRepository;
    private StudentProfileRepository studentProfileRepository;
    private PasswordEncoder passwordEncoder;
    private GuardianConsentService guardianConsentService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mfaFactorRepository = mock(MfaFactorRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AesGcmEncryptionUtil encryptionUtil = mock(AesGcmEncryptionUtil.class);
        MfaRateLimitService mfaRateLimitService = mock(MfaRateLimitService.class);
        AccountStatusService accountStatusService = mock(AccountStatusService.class);
        guardianConsentService = mock(GuardianConsentService.class);

        authService = new AuthService(
                userRepository,
                mfaFactorRepository,
                studentProfileRepository,
                passwordEncoder,
                jwtTokenProvider,
                encryptionUtil,
                mfaRateLimitService,
                accountStatusService,
                guardianConsentService,
                Optional.empty());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted-secret");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.aistudymentor.auth.entity.User user = invocation.getArgument(0);
            user.setUserId(UUID.randomUUID());
            return user;
        });
    }

    @Test
    void underThirteenRegistrationRequiresAndStartsPendingGuardianConsent() {
        RegisterRequest request = request(LocalDate.now().minusYears(12), "parent@example.test");

        RegisterResponse response = authService.register(request);

        assertEquals(AccountStatus.PENDING_CONSENT, response.getAccountStatus());
        assertTrue(response.isGuardianConsentRequired());
        assertEquals("PENDING", response.getConsentStatus().name());
        verify(guardianConsentService).initiateConsentFlow(any(), org.mockito.ArgumentMatchers.eq("parent@example.test"));
    }

    @Test
    void underThirteenRegistrationCannotProceedWithoutGuardianEmail() {
        RegisterRequest request = request(LocalDate.now().minusYears(12), null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals("GUARDIAN_EMAIL_REQUIRED", exception.getCode());
        verify(userRepository, never()).save(any());
        verify(guardianConsentService, never()).initiateConsentFlow(any(), anyString());
    }

    @Test
    void usersAtLeastThirteenDoNotEnterGuardianPendingState() {
        RegisterRequest request = request(LocalDate.now().minusYears(13), null);

        RegisterResponse response = authService.register(request);

        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        assertFalse(response.isGuardianConsentRequired());
        verify(guardianConsentService, never()).initiateConsentFlow(any(), anyString());
    }

    private RegisterRequest request(LocalDate dob, String guardianEmail) {
        return RegisterRequest.builder()
                .email("student@example.test")
                .password("test-password-123")
                .dob(dob)
                .guardianEmail(guardianEmail)
                .educationLevel("middle-school")
                .build();
    }
}