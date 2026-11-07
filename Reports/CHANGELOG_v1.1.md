# Changelog

## [v1.2] - 2026-11-07 (PO Review Remediation)

### Added
- **Flyway Migrations**: Added versioned migrations `V2__Fix_Core_Security.sql`, `V3__Question_And_Ai_Schema.sql`, and `V4__Quiz_Schema.sql`.
- **MFA Rate Limiting & Audit**: Added `mfa_attempts` table, `MfaRateLimitService`, and 5-attempts/15-minute brute-force lockout.
- **Guardian Consent Loop**: Added 72-hour signed token email verification loop with SHA-256 `token_hash`, resend counts, and IP audit logging.
- **Mailpit Integration**: Added Mailpit local SMTP (`1025`) and Web UI (`8025`) container in `docker-compose.yml`.
- **Testcontainers Integration**: Added `FlywaySchemaValidationIntegrationTest` validating Hibernate against real MySQL 8.
- **Unit & Security Tests**: Added `SecurityRulesUnitTest` asserting all D5 401/403 rules, pre-MFA token isolation, U13 consent blocking, and registration age validation.
- **JSON Schema**: Added `docs/ai_response.schema.json` formalizing strict structured output from Google Gemini.
- **Security Utilities**: Added `AesGcmEncryptionUtil` for AES-GCM TOTP secret encryption. Added `CustomAccessDeniedHandler`.
- **Pending Decisions Document**: Created `PO_DECISIONS_PENDING.md` cataloging open items (D3 Gemini model, legal review for D2).
- **Remediation Report**: Created `FIX_REPORT.md` mapping all findings (B1-B4, H1-H9, docs) with status and evidence.

### Changed
- **Repo Hygiene**: Consolidated single `docker-compose.yml` and `.env.example` in `/backend`. Removed duplicate configs, takeout directory, and zip files. Added `.gitignore`.
- **MFA Factor**: Enforced TOTP only (RFC 6238, Decision D1). Removed all "resend code" concepts from specs and UI.
- **Account Status**: Standardized on `ACTIVE`, `PENDING_CONSENT`, and `SUSPENDED` (Decision D6). Purged `PENDING_MFA`.
- **Bookmarks**: Retained only `question_bookmarks` table. Removed `is_bookmarked` column from `questions` table (Decision D7).
- **Environment & Secrets**: Removed all hardcoded secrets and default values for `JWT_SECRET`, `DB_PASSWORD`, `GEMINI_*`, and `MFA_ENCRYPTION_KEY`. Added fail-fast startup check for JWT secrets < 32 bytes (Decision D3, D4, WP4).
- **API Spec**: Expanded with 13 previously missing endpoints, standard pagination, and standard `ErrorResponse`.
- **Agent Rules**: Updated ownership boundaries to match actual project package paths.

---

## [v1.1] - 2026-11-07

## Added
- **MFA Specifications**: Documented MFA enrollment, challenge, and verification flows. Added `mfa_factors` DB entity and `/api/v1/auth/mfa` APIs.
- **Under-13 Guardian Consent**: Added privacy/consent workflow, `guardian_consents` DB entity, and strict authorization blocks for pending accounts.
- **Extended MVP (Quizzes & Dashboard)**: Formalized FR-07 and FR-08. Added practice quizzes, attempts, grading logic, and progress telemetry.
- **Agent Personas**: Generated missing Agent documentation for AI/Data Lead, Android Lead, Backend Persona, QA Persona.
- **Validation Reports**: Added Source Audit, Conflict, and Validation reports.

## Changed
- **AI Provider**: Officially set to Google Gemini. Refactored `AI_ORCHESTRATOR_SPEC.md` to reference Gemini specific constraints.
- **MVP Scope**: Promoted Practice Quizzes and Progress Dashboard from 'Conditional' to 'Core Extended MVP'.
- **Database Design**: Expanded schema to include Quizzes, Attempts, MFA, and Consent logs.
