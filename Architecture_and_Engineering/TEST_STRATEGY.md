# Test Strategy v1.2

This strategy outlines the automated verification pipeline, security assertions, and quality standards for the AI Study Mentor project.

---

## 1. Security & Compliance Tests

### 1.1 Authentication & Token Lifecycle Tests
* **Pre-MFA Token Restrictiveness (D5)**: Attempt to invoke protected endpoints (e.g., `/api/v1/questions`, `/api/v1/quizzes`) using an `MFA_SESSION` temporary token.
  * *Expected Result*: HTTP 403 Forbidden with `code: "MFA_REQUIRED"`.
* **Expired Token Handling (D5)**: Pass an expired JWT access token.
  * *Expected Result*: HTTP 401 Unauthorized with `code: "TOKEN_EXPIRED"`.
* **Invalid Signature / Malformed Token (D5)**: Pass tampered or malformed Bearer tokens.
  * *Expected Result*: HTTP 401 Unauthorized with `code: "INVALID_TOKEN"` or `"UNAUTHORIZED"`.

### 1.2 Under-13 & Account Status Enforcement Tests
* **Consent Bypass**: Attempt to access `/api/v1/questions` or submit quizzes using an account with `account_status == PENDING_CONSENT`.
  * *Expected Result*: HTTP 403 Forbidden with `code: "GUARDIAN_CONSENT_REQUIRED"`.
* **Whitelisted Routes for U13**: Verify that `GET /api/v1/users/me/guardian-consent` and `GET/PUT /api/v1/users/me/profile` remain accessible during `PENDING_CONSENT`.
* **Suspended Account Blocker**: User with `account_status == SUSPENDED` attempting any action.
  * *Expected Result*: HTTP 403 Forbidden with `code: "ACCOUNT_SUSPENDED"`.
* **Real-time Invalidation (H1)**: Approve guardian consent in the database; subsequent request without relogin must immediately succeed due to Redis/DB cache invalidation.

### 1.3 MFA Brute-Force & Rate-Limiting Tests (H6)
* **Brute-Force Lockout**: Execute 5 consecutive failed TOTP verification attempts on `POST /auth/mfa/verify`.
  * *Expected Result*: The 6th attempt is blocked with HTTP 429 Too Many Requests (`code: "MFA_RATE_LIMITED"`) or 403 lockout for 15 minutes.
* **Audit Trail**: Assert that every attempt creates an entry in `mfa_attempts` recording `user_id`, `success`, `ip`, and timestamp.

### 1.4 Insecure Direct Object Reference (IDOR) & Ownership Tests
* **Cross-Profile Question Access**: User A attempts to view, bookmark, or delete questions belonging to User B (`profile_id`).
  * *Expected Result*: HTTP 403 Forbidden or HTTP 404 Not Found.
* **Firebase Storage Path Ownership**: Client requests Q&A for an image path under another user's prefix (`users/{otherUserId}/images/...`).
  * *Expected Result*: HTTP 403 Forbidden.

### 1.5 Quiz Integrity & Answer Leakage Tests (D8)
* **Quiz Answer Leakage**: Inspect `GET /quizzes/{id}` and `POST /quizzes` JSON response payloads.
  * *Expected Result*: Assert that `correct_answer` or `correctOptionId` does not exist anywhere in the serialized JSON.
* **Short-Answer Normalization**: Submit answer with varying whitespace and capitalization (e.g., "  photosynthesis  " vs "Photosynthesis").
  * *Expected Result*: Successfully graded as correct.

---

## 2. Infrastructure & Schema Tests

### 2.1 Flyway + JPA Validation Integration Test
* **Testcontainers Integration**:
  - Spin up official `mysql:8.0.36` container via Testcontainers.
  - Run all Flyway migrations (`V1` to `V4`).
  - Boot Spring Application Context with `spring.jpa.hibernate.ddl-auto=validate`.
  - Assert that all JPA entities, column types, and constraints match the physical MySQL schema exactly.

### 2.2 AI Schema & Fault-Tolerance Tests
* **Strict JSON Validation (NFR-01)**: Feed malformed, incomplete, or non-JSON strings to the AI Orchestrator adapter.
  * *Expected Result*: Throws validation exception and rejects response.
* **Fail-Fast Unconfigured AI (D3)**: Invoke AI service when `GEMINI_API_KEY` or `GEMINI_MODEL` equals `<SET_ME>`.
  * *Expected Result*: Throws `AiNotConfiguredException` with `code: "AI_NOT_CONFIGURED"`.

---

## 3. Code Coverage Targets

* **Security Filters & Handlers**: 100% line coverage.
* **Authentication & Rate-Limit Services**: >90% branch coverage.
* **Data Repositories & JPA Entities**: 100% schema validation passing via Testcontainers.
* **Overall Backend Line Coverage Target**: >80%.
