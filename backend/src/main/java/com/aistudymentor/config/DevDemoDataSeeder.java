package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.MfaFactor;
import com.aistudymentor.auth.entity.MfaType;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.MfaFactorRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.auth.service.GuardianConsentService;
import com.aistudymentor.common.util.AesGcmEncryptionUtil;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MfaFactorRepository mfaFactorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesGcmEncryptionUtil encryptionUtil;
    private final GuardianConsentService guardianConsentService;

    @Value("${app.dev-seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.dev-seed.password}")
    private String demoPassword;

    @Value("${app.dev-seed.totp-secret}")
    private String demoTotpSecret;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        List<DemoUser> demoUsers = List.of(
                new DemoUser("demo.middle@local.test", "Middle School", 15, AccountStatus.ACTIVE, null),
                new DemoUser("demo.high@local.test", "High School", 17, AccountStatus.ACTIVE, null),
                new DemoUser("demo.university@local.test", "University", 21, AccountStatus.ACTIVE, null),
                new DemoUser("demo.pending@local.test", "High School", 12, AccountStatus.PENDING_CONSENT,
                        "guardian@local.test"));

        for (DemoUser demo : demoUsers) {
            if (userRepository.existsByEmail(demo.email())) {
                continue;
            }

            User user = userRepository.save(User.builder()
                    .email(demo.email())
                    .passwordHash(passwordEncoder.encode(demoPassword))
                    .dob(LocalDate.now().minusYears(demo.age()))
                    .accountStatus(demo.accountStatus())
                    .build());

            studentProfileRepository.save(StudentProfile.builder()
                    .user(user)
                    .educationLevel(demo.educationLevel())
                    .build());

            mfaFactorRepository.save(MfaFactor.builder()
                    .user(user)
                    .type(MfaType.TOTP)
                    .secret(encryptionUtil.encrypt(demoTotpSecret))
                    .isVerified(true)
                    .build());

            if (demo.guardianEmail() != null) {
                guardianConsentService.initiateConsentFlow(user, demo.guardianEmail());
            }
        }
    }

    private record DemoUser(
            String email,
            String educationLevel,
            int age,
            AccountStatus accountStatus,
            String guardianEmail) {
    }
}
