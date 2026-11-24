package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.MfaFactor;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.MfaFactorRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.auth.service.GuardianConsentService;
import com.aistudymentor.common.util.AesGcmEncryptionUtil;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class DevDemoDataSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudentProfileRepository profileRepository = mock(StudentProfileRepository.class);
    private final MfaFactorRepository mfaFactorRepository = mock(MfaFactorRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AesGcmEncryptionUtil encryptionUtil = mock(AesGcmEncryptionUtil.class);
    private final GuardianConsentService guardianConsentService = mock(GuardianConsentService.class);

    @Test
    void seedsThreeEducationLevelsAndOnePendingMinorOnlyWhenEnabled() {
        DevDemoDataSeeder seeder = newSeeder(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.randomUUID());
            return user;
        });
        when(passwordEncoder.encode("local-demo-password")).thenReturn("encoded");
        when(encryptionUtil.encrypt("JBSWY3DPEHPK3PXP")).thenReturn("encrypted-demo-secret");

        seeder.run(new DefaultApplicationArguments());

        verify(userRepository, times(4)).save(any(User.class));
        verify(profileRepository, times(4)).save(any(StudentProfile.class));
        verify(mfaFactorRepository, times(4)).save(any(MfaFactor.class));
        verify(guardianConsentService).initiateConsentFlow(
                org.mockito.ArgumentMatchers.argThat(user -> user.getAccountStatus() == AccountStatus.PENDING_CONSENT),
                eq("guardian@local.test"));
    }

    @Test
    void doesNotCreateDemoUsersWhenDisabled() {
        DevDemoDataSeeder seeder = newSeeder(false);

        seeder.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any(User.class));
        verify(profileRepository, never()).save(any(StudentProfile.class));
        verify(mfaFactorRepository, never()).save(any(MfaFactor.class));
        verify(guardianConsentService, never()).initiateConsentFlow(any(), any());
    }

    private DevDemoDataSeeder newSeeder(boolean enabled) {
        DevDemoDataSeeder seeder = new DevDemoDataSeeder(
                userRepository,
                profileRepository,
                mfaFactorRepository,
                passwordEncoder,
                encryptionUtil,
                guardianConsentService);
        ReflectionTestUtils.setField(seeder, "seedEnabled", enabled);
        ReflectionTestUtils.setField(seeder, "demoPassword", "local-demo-password");
        ReflectionTestUtils.setField(seeder, "demoTotpSecret", "JBSWY3DPEHPK3PXP");
        return seeder;
    }
}
