# ADR-0002: TOTP Multi-Factor Authentication and One-Time Recovery Codes

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Account credentials alone are insufficient protection for student accounts. The product requires a second factor, protection against repeated verification attempts, and a recovery path that does not make reusable plaintext bypass codes available.

## 2. Decision Drivers & Alternatives Considered

- Use time-based one-time passwords (TOTP) as the MFA factor.
- Protect stored TOTP secrets with authenticated encryption.
- Limit repeated failures by account and source IP.
- Issue ten single-use recovery codes and store only password hashes of those codes.
- Alternatives considered: email “resend code” MFA, reusable recovery codes, and unlimited verification attempts.

## 3. Decision Outcome & Justification

Use TOTP (RFC 6238) with encrypted secrets. Permit no more than five failed MFA attempts per user or IP within a rolling 15-minute window. Provide ten one-time recovery codes, storing only their hashes and invalidating each code after use.

The backend implements the policy: TOTP secrets are encrypted, MFA failures are limited to five per user or source IP within 15 minutes, and ten recovery codes are generated and stored as BCrypt hashes. A recovery code is consumed after successful use. Authenticated endpoints require password and TOTP to regenerate codes or disable MFA. Automated backend tests cover recovery-code single-use and related identity behavior; live Android-to-backend verification remains outstanding.

## 4. Consequences

- **Positive**: TOTP avoids dependence on email delivery for each login, encrypted secrets reduce exposure at rest, and rate limits slow brute-force attempts.
- **Negative**: Users must retain access to an authenticator or securely retain recovery codes. Lost factors require a controlled account recovery process.
- Recovery codes add persistent state and require users to retain the plaintext codes shown once during generation.

## 5. References & Code Artifacts

- [AuthService.java](../../backend/src/main/java/com/aistudymentor/auth/service/AuthService.java)
- [MfaRateLimitService.java](../../backend/src/main/java/com/aistudymentor/auth/service/MfaRateLimitService.java)
- [AesGcmEncryptionUtil.java](../../backend/src/main/java/com/aistudymentor/common/util/AesGcmEncryptionUtil.java)
- [PasswordRecoveryService.java](../../backend/src/main/java/com/aistudymentor/auth/service/PasswordRecoveryService.java)
- [MfaRecoveryCode.java](../../backend/src/main/java/com/aistudymentor/auth/entity/MfaRecoveryCode.java)
- [Identity migration V5](../../backend/src/main/resources/db/migration/V5__Identity_Journey.sql)
- [MFA schema migration](../../backend/src/main/resources/db/migration/V1__Init_Core_Security_Schema.sql)
- [MFA attempt migration](../../backend/src/main/resources/db/migration/V2__Fix_Core_Security.sql)
