package com.aistudymentor.auth.service;

import com.aistudymentor.auth.dto.LoginRequest;
import com.aistudymentor.auth.dto.LoginResponse;
import com.aistudymentor.auth.dto.MfaVerifyRequest;
import com.aistudymentor.auth.dto.MfaVerifyResponse;
import com.aistudymentor.auth.dto.RefreshTokenRequest;
import com.aistudymentor.auth.dto.RegisterRequest;
import com.aistudymentor.auth.dto.RegisterResponse;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.ConsentStatus;
import com.aistudymentor.auth.entity.MfaFactor;
import com.aistudymentor.auth.entity.MfaType;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.MfaFactorRepository;
import com.aistudymentor.auth.repository.UserRepository;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.config.JwtTokenProvider;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.aistudymentor.common.util.AesGcmEncryptionUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // Spring Data repository nullability annotations are not fully inferred by the IDE.
public class AuthService {

    private static final int MINOR_AGE_THRESHOLD = 13;
    private static final String REFRESH_BLOCKLIST_PREFIX = "refresh:blocklist:";
    private static final long REFRESH_TTL_SECONDS = 7 * 24 * 3600L; // 7 days

    private final UserRepository userRepository;
    private final MfaFactorRepository mfaFactorRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AesGcmEncryptionUtil aesGcmEncryptionUtil;
    private final MfaRateLimitService mfaRateLimitService;
    private final AccountStatusService accountStatusService;
    private final GuardianConsentService guardianConsentService;
    private final Optional<StringRedisTemplate> redisTemplate;

    // ─────────────────────────── REGISTER ───────────────────────────

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 1. Duplicate email check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "An account with this email already exists.");
        }

        // 2. Determine if minor (D2)
        boolean isMinor = isMinor(request.getDob());
        if (isMinor && (request.getGuardianEmail() == null || request.getGuardianEmail().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "GUARDIAN_EMAIL_REQUIRED",
                "A guardian email is required for users under 13 years old.");
        }
        if (isMinor && request.getGuardianEmail().trim().equalsIgnoreCase(request.getEmail().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "GUARDIAN_EMAIL_MUST_DIFFER",
                "Guardian email must be different from the student email.");
        }
        boolean requiresConsent = isMinor;

        // 3. Set initial AccountStatus
        AccountStatus initialStatus = requiresConsent
                ? AccountStatus.PENDING_CONSENT
                : AccountStatus.ACTIVE;

        // 4. Build & save User
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .dob(request.getDob())
                .accountStatus(initialStatus)
                .build();
        user = userRepository.save(user);

        // 5. Create StudentProfile (PD-004)
        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .educationLevel(request.getEducationLevel())
                .build();
        studentProfileRepository.save(profile);

        // 6. Generate TOTP secret & save MfaFactor (PD-005: mandatory enrollment)
        String totpSecret = new DefaultSecretGenerator().generate();
        String encryptedSecret = aesGcmEncryptionUtil.encrypt(totpSecret);
        MfaFactor mfaFactor = MfaFactor.builder()
                .user(user)
                .type(MfaType.TOTP)
                .secret(encryptedSecret)
                .isVerified(false)
                .build();
        mfaFactorRepository.save(mfaFactor);

        // 7. If minor, send guardian consent email
        ConsentStatus consentStatus = null;
        if (requiresConsent) {
            guardianConsentService.initiateConsentFlow(user, request.getGuardianEmail());
            consentStatus = ConsentStatus.PENDING;
        }

        log.info("User registered: userId={}, status={}, minor={}", user.getUserId(), initialStatus, isMinor);

        return RegisterResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .accountStatus(initialStatus)
                .consentStatus(consentStatus)
                .mfaRequired(true)
                .guardianConsentRequired(requiresConsent)
                .message(requiresConsent
                        ? "Registration successful. Guardian consent email sent to " + request.getGuardianEmail() + ". Please check your authenticator app to complete MFA enrollment."
                        : "Registration successful. Please complete MFA enrollment using your authenticator app.")
                .build();
    }

    // ─────────────────────────── LOGIN ───────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        // 1. Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid email or password."));

        // 2. Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password.");
        }

        // 3. Check account not suspended
        AccountStatus liveStatus = accountStatusService.getAccountStatus(user.getUserId());
        if (liveStatus == AccountStatus.SUSPENDED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "Your account has been suspended.");
        }

        // 4. Check if MFA factor is verified (enrolled)
        Optional<MfaFactor> totpFactor = mfaFactorRepository.findByUserAndType(user, MfaType.TOTP);
        boolean mfaEnrolled = totpFactor.map(MfaFactor::getIsVerified).orElse(false);

        if (mfaEnrolled) {
            // Issue MFA_SESSION token; full access after verify
            String mfaSessionToken = jwtTokenProvider.generateMfaSessionToken(user.getUserId(), user.getEmail());
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .mfaRequired(true)
                    .mfaSessionToken(mfaSessionToken)
                    .build();
        } else {
            // MFA not yet enrolled → issue full tokens (enrollment was forced at register)
            // This handles the edge case where enrollment wasn't completed
            String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail(), liveStatus);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), user.getEmail());
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .mfaRequired(false)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();
        }
    }

    // ─────────────────────────── MFA VERIFY ───────────────────────────

    @Transactional
    public MfaVerifyResponse verifyMfa(MfaVerifyRequest request, String clientIp) {
        // 1. Parse & validate MFA_SESSION token
        String sessionToken = request.getMfaSessionToken();
        if (!jwtTokenProvider.validateToken(sessionToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_MFA_SESSION",
                    "MFA session token is invalid or expired.");
        }
        String tokenType = jwtTokenProvider.getTokenType(sessionToken);
        if (!"MFA_SESSION".equals(tokenType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN_TYPE",
                    "Provided token is not an MFA session token.");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(sessionToken);
        String email = jwtTokenProvider.getEmailFromToken(sessionToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                        "User associated with this session no longer exists."));

        // 2. Rate limit check (PD-006)
        if (mfaRateLimitService.isRateLimited(user, clientIp)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "MFA_RATE_LIMITED",
                    "Too many failed MFA attempts. Please wait 15 minutes before trying again.");
        }

        // 3. Find TOTP factor
        MfaFactor factor = mfaFactorRepository.findByUserAndType(user, MfaType.TOTP)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "MFA_NOT_ENROLLED",
                        "No MFA factor found. Please re-register or contact support."));

        // 4. Decrypt & verify TOTP code
        String plainSecret = aesGcmEncryptionUtil.decrypt(factor.getSecret());
        boolean codeValid = verifyTotp(plainSecret, request.getCode());

        mfaRateLimitService.recordAttempt(user, codeValid, clientIp);

        if (!codeValid) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_MFA_CODE",
                    "The provided MFA code is invalid.");
        }

        // 5. Mark factor as verified if not already (first enrollment confirmation)
        if (!factor.getIsVerified()) {
            factor.setIsVerified(true);
            mfaFactorRepository.save(factor);
        }

        // 6. Issue full tokens
        AccountStatus liveStatus = accountStatusService.getAccountStatus(userId);
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email, liveStatus);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        log.info("MFA verified successfully for userId={}", userId);

        return MfaVerifyResponse.builder()
                .userId(userId)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();
    }

    // ─────────────────────────── REFRESH TOKEN ───────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Validate token format & signature
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                    "Refresh token is invalid or expired.");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN_TYPE",
                    "Provided token is not a refresh token.");
        }

        // 2. PD-003: Check Redis blocklist (Option A)
        String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
        if (isTokenRevoked(jti)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED",
                    "This refresh token has been revoked.");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 3. Confirm user still exists
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found.");
        }

        AccountStatus liveStatus = accountStatusService.getAccountStatus(userId);
        if (liveStatus == AccountStatus.SUSPENDED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "Your account has been suspended.");
        }

        // 4. Issue new tokens (refresh token rotation)
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email, liveStatus);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        // 5. Blocklist old refresh token JTI
        revokeToken(jti);

        log.info("Refresh token rotated for userId={}", userId);

        return LoginResponse.builder()
                .userId(userId)
                .email(email)
                .mfaRequired(false)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();
    }

    // ─────────────────────────── LOGOUT ───────────────────────────

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return; // idempotent
        }
        try {
            if (jwtTokenProvider.validateToken(refreshToken)) {
                String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
                revokeToken(jti);
                log.info("Logout: refresh token jti={} revoked", jti);
            }
        } catch (Exception ex) {
            log.warn("Logout token revocation failed ({})", ex.getClass().getSimpleName());
        }
    }

    // ─────────────────────────── HELPERS ───────────────────────────

    private boolean isMinor(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears() < MINOR_AGE_THRESHOLD;
    }

    private boolean verifyTotp(String secret, String code) {
        try {
            CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
            CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
            return verifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.error("TOTP verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenRevoked(String jti) {
        if (redisTemplate.isPresent()) {
            try {
                return Boolean.TRUE.equals(redisTemplate.get().hasKey(REFRESH_BLOCKLIST_PREFIX + jti));
            } catch (Exception ex) {
                log.warn("Redis unavailable for refresh-token revocation check ({})",
                        ex.getClass().getSimpleName());
            }
        }
        return false;
    }

    private void revokeToken(String jti) {
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(
                        REFRESH_BLOCKLIST_PREFIX + jti,
                        "1",
                        REFRESH_TTL_SECONDS,
                        TimeUnit.SECONDS
                );
            } catch (Exception ex) {
                log.warn("Redis unavailable; refresh-token revocation could not be stored ({})",
                        ex.getClass().getSimpleName());
            }
        }
    }
}
