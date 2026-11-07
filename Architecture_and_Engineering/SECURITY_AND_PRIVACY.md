# Security and Privacy v1.2

This document details the mandatory security and compliance policies for the AI Study Mentor platform.

---

## 1. Zero-Trust API Key & Secrets Isolation
* **Zero Client Secrets**: Under no circumstances may AI API keys (Google Gemini), database credentials, JWT secrets, or MFA encryption keys be bundled into the Android APK, Committed to Git, or exposed through client network traffic.
* **Environment Configuration**: All secrets are supplied strictly via environment variables (`.env` in development, container secrets in production).
* **Fail-Fast Startup Enforcement**: The application fails immediately on startup if `JWT_SECRET` is shorter than 32 bytes (256 bits). AI invocations fail fast with `AI_NOT_CONFIGURED` if model credentials are unset (Decision D3).

---

## 2. Under-13 Protection & Guardian Consent Workflow (Decision D2, D9)
* **Age Verification**: Evaluated dynamically based on `dob` at registration time. Users under 13 are created with `account_status == PENDING_CONSENT`.
* **72-Hour Email Loop**:
  - The backend generates a signed single-use verification token valid for 72 hours.
  - The token is stored in the database exclusively as an SHA-256 hash (`token_hash CHAR(64)`).
  - Resend attempts are tracked (`resend_count`, `last_sent_at`) and rate-limited.
  - Verification logs client IP (`approved_ip`) and timestamp (`verified_at`).
* **Route Enforcement Whitelist (H5)**:
  - While in `PENDING_CONSENT`, users are restricted **only** to:
    1. Authentication endpoints (`/api/v1/auth/**`)
    2. Consent status check (`GET /api/v1/users/me/guardian-consent`)
    3. Profile read and preferences update (`GET/PUT /api/v1/users/me/profile`)
    4. Error handling (`/error`)
    5. Health check (`/actuator/health`)
  - All other core features (Questions, Answers, Quizzes, Dashboard) return **HTTP 403 Forbidden** with `code: "GUARDIAN_CONSENT_REQUIRED"`.
* **Legal Disclaimer**: *Legal review pending - current implementation is not certified for production COPPA/GDPR-K compliance*.

---

## 3. Multi-Factor Authentication & Brute-Force Defense (Decision D1, H6)
* **TOTP Standard**: Exclusively utilizes Time-Based One-Time Password (RFC 6238). All legacy "resend code" behaviors are abolished.
* **Secret Encryption**: TOTP secret keys are stored in `mfa_factors.secret` encrypted with AES-GCM (128-bit authentication tag, 12-byte initialization vector) using `MFA_ENCRYPTION_KEY`.
* **Brute-Force Rate Limiting**:
  - Max 5 failed TOTP verification attempts within a 15-minute sliding window per user or per client IP.
  - The 6th attempt triggers an immediate 15-minute lockout returning HTTP 429 Too Many Requests (`code: "MFA_RATE_LIMITED"`).
  - All verification events are audited in `mfa_attempts`.
* **Token Isolation**:
  - During login challenge, the server issues an `MFA_SESSION` temporary token.
  - Submitting an `MFA_SESSION` token to any protected core route returns **HTTP 403 Forbidden** with `code: "MFA_REQUIRED"`. Full `ACCESS` tokens are issued only upon valid TOTP verification.

---

## 4. Strict Profile Ownership & IDOR Defense
* **Data Ownership Binding**: All academic records (`questions`, `question_attachments`, `answers`, `question_bookmarks`, `practice_quizzes`, `quiz_attempts`, `ai_generation_logs`) are bound to `student_profiles.profile_id`.
* **Query Scope**: Every database retrieval automatically validates that the requesting `profile_id` matches the authenticated subject.

---

## 5. Storage & Quiz Integrity
* **Firebase Storage Security Rules**: Images reside in private paths `users/{userId}/images/{imageId}`. Direct client reads/writes are strictly restricted to the resource owner; backend verifies ownership via Firebase Admin SDK before processing.
* **Server-Side Grading Integrity (Decision D8)**:
  - Correct answers reside strictly in `quiz_questions.correct_answer` on the server and are stripped from all quiz retrieval DTOs.
  - Short-answer responses are evaluated on the server using normalized string comparison (trim, lowercase, collapsed internal whitespace).
