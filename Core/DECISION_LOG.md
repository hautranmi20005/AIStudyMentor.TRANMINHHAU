# Architectural Decision Log v1.2

| ID | Decision | Status | Notes & PO Decisions |
|---|---|---|---|
| ADR-001 | Native Android (Kotlin/Compose) | Confirmed | Android Studio, Material 3, Navigation Compose. Replaces Flutter/iOS. |
| ADR-002 | State Management: MVVM + StateFlow | Confirmed | Standard Jetpack architecture with Hilt DI. |
| ADR-003 | Local Caching: Room Database | Confirmed | Client-side offline cache for questions and history. |
| ADR-004 | Java + Spring Boot Backend | Confirmed | Modular monolith architecture. Microservices banned. |
| ADR-005 | MySQL + Redis + Firebase Storage | Confirmed | MySQL 8.x relational persistence, Redis 7 caching, Firebase for image storage & FCM. |
| ADR-006 | Database Migrations: Flyway | Confirmed | Versioned SQL migrations (V1..V4). Never mutate applied migrations. |
| ADR-007 | API Contracts & DTO Decoupling | Confirmed | JPA Entities must never be exposed as DTOs. UUID mapped to VARCHAR(36). |
| ADR-008 | AI Provider: Google Gemini | **Confirmed** | Backend adapter only. Model & API version: **TBD (<SET_ME>, Decision D3)**; fails fast with `AI_NOT_CONFIGURED`. |
| ADR-009 | MFA Factor: TOTP (RFC 6238) | **Confirmed** | **Decision D1**: Pure TOTP with AES-GCM encrypted secrets. All "resend code" concepts purged. Max 5 failed attempts per 15 min lock. |
| ADR-010 | Under-13 Guardian Verification | **Confirmed** | **Decision D2**: 72-hour signed single-use token email loop with SHA-256 token hash & audit fields. Dev via Mailpit. *(Note: legal review pending - not production-ready)*. |
| ADR-011 | Extended MVP Scope | **Confirmed** | Includes Practice Quizzes (FR-07) and Progress Dashboard (FR-08). |
| ADR-012 | Token Lifetimes & Rotation | **Confirmed** | **Decision D4 & H2**: Access token 15m, Refresh token 7d (with jti, rotation & revocation), MFA session 5m. |
| ADR-013 | Error Handling & Security Codes | **Confirmed** | **Decision D5**: Standard `ErrorResponse` structure. 401 for invalid/expired tokens, 403 for `MFA_REQUIRED`, `GUARDIAN_CONSENT_REQUIRED`, `ACCOUNT_SUSPENDED`. |
| ADR-014 | Account Status Enumeration | **Confirmed** | **Decision D6**: `ACTIVE`, `PENDING_CONSENT`, `SUSPENDED`. `PENDING_MFA` purged. Status loaded from DB/Redis cache. |
| ADR-015 | Bookmarks Data Architecture | **Confirmed** | **Decision D7**: Dedicated `question_bookmarks` table. Column `is_bookmarked` purged from `questions` table. |
| ADR-016 | Server-Side Quiz Grading Integrity | **Confirmed** | **Decision D8**: `quiz_questions.correct_answer` strictly server-side, never in DTOs. Normalized string comparison for short-answers. |
| ADR-017 | Age Calculation Standard | **Confirmed** | **Decision D9**: Age < 13 calculated dynamically from `dob` at registration time. |
