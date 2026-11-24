# Project Context

## 1. Purpose and evidence policy

AI Study Mentor is a student-focused Android application backed by a Java/Spring Boot API. The Android app supports account access, asking text questions, quizzes, progress metrics, study history, and account settings. The backend owns authentication, persistence, AI-provider calls, quiz grading, and account-level policy enforcement.

This document describes the current implementation in `backend/src/main`, `android/app/src/main`, build files, and migrations. “Implemented” means a source path is wired into the current application; “Partially implemented” means relevant code exists but the normal application flow is incomplete or disconnected. Roadmap documents are not treated as proof of functionality. External service availability and deployment state require manual verification.

## 2. Technology stack

| Area | Declared technology |
|---|---|
| Backend language/runtime | Java 17; Spring Boot 3.3.2 (`backend/pom.xml`) |
| Backend web/security | Spring MVC, Jakarta Bean Validation, Spring Security, stateless JWT authentication |
| Backend persistence | Spring Data JPA/Hibernate; Flyway migrations; H2, MySQL and PostgreSQL JDBC drivers |
| Backend integrations | Redis, Java Mail, Google Firebase Admin, Google Gemini REST API |
| Backend utilities | Jackson, Lombok, TOTP library, JJWT, Spring Actuator, Logstash Logback encoder |
| Android | Kotlin 1.9.22, Android Gradle Plugin 8.3.0, KSP, Java/Kotlin target 17 |
| Android UI/navigation | Jetpack Compose, Material 3, Navigation Compose, Android lifecycle/ViewModel |
| Android networking | Retrofit 2.9.0, Gson converter, OkHttp 4.12.0 |
| Android local/background work | Room 2.6.1, WorkManager 2.9.0, Kotlin coroutines |
| Android other dependencies | AndroidX Security Crypto, Coil, ExifInterface, Firebase Analytics and Crashlytics |
| Tests | JUnit 5/Spring Boot test and Testcontainers on backend; JUnit 4, Compose UI tests, Room tests and MockWebServer on Android |

Firebase Android plugins are conditionally enabled when `android/app/google-services.json` exists. Release Android builds require signing configuration; the release API base URL is configurable and defaults to a placeholder URL. Declared versions are taken from `backend/pom.xml`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, and `android/gradle/wrapper/gradle-wrapper.properties`; this table does not assert that every toolchain combination has been built in the current environment.

## 3. Repository layout

| Path | Contents |
|---|---|
| `backend/src/main/java/com/aistudymentor/` | Spring Boot modular-monolith packages: `account`, `ai`, `auth`, `common`, `config`, `dashboard`, `notification`, `profile`, `question`, and `quiz` |
| `backend/src/main/resources/` | Spring configuration, MySQL/H2 and PostgreSQL Flyway migrations, and HTML templates for email/guardian consent |
| `backend/src/test/java/` | Backend unit, security, service, controller, schema, and end-to-end tests |
| `android/app/src/main/java/com/aistudymentor/ui/` | Compose screens, ViewModels, navigation, shared UI and theme |
| `android/app/src/main/java/com/aistudymentor/data/` | Retrofit API, repositories, Room cache, token storage, synchronization, notifications, and telemetry |
| `android/app/src/test/` | Android/JVM unit tests |
| `android/app/src/androidTest/` | Emulator/device Compose, Room, and network-contract tests |
| `scripts/` | Local development, demo, backend, service, and smoke-test scripts |
| `docs/`, `Core/`, `Architecture_and_Engineering/`, `Reports/` | Project specifications and operational reports; these are secondary to the executable source for implementation claims |
| `storage.rules` | Firebase Storage rules |
| `.github/workflows/` | CI and Android beta distribution workflows |
| `docker-compose.dev.yml`, `backend/docker-compose*.yml` | Local development and backend deployment Compose definitions |

There is no separate web front-end project in the source layout. The browser-facing HTML is limited to backend-rendered guardian-consent pages and email content.

## 4. Runtime architecture and configuration

```text
Android Compose UI
  -> ViewModels and repositories
  -> Retrofit/OkHttp (Bearer access token)
  -> Spring Security filters and REST controllers
  -> modular services
  -> JPA/Flyway database, Redis, mail, Gemini, and Firebase integrations

Android local data:
Room cache <- OfflineRepository <- Retrofit
WorkManager <- OfflineSyncWorker / LocalNotificationWorker
```

The Android app builds one application module (`android/app`). `AppContainer` and `AppViewModelFactory` provide dependencies; the current Gradle dependencies do not include Hilt.

| Runtime | Source-backed configuration |
|---|---|
| Default backend configuration | `application.yml` defaults to H2 in-memory, Redis on localhost, `AI_PROVIDER=gemini`, Gemini API version `v1beta`, and a configured primary model name. |
| Development | `application-dev.yml` requires database, Redis, JWT/MFA, and seed settings from environment; the repository’s root development Compose stack provides MySQL, Redis, and Mailpit. |
| Production | `application-prod.yml` selects PostgreSQL migrations, Redis, SMTP/SendGrid settings, and a loopback-bound management port. Production Compose is under `backend/`. |
| Gemini | `GeminiAdapterService` uses the hard-coded Google Generative Language API base `https://generativelanguage.googleapis.com`, constructs `/{apiVersion}/models/{model}:generateContent`, and sends JSON headers. The configured primary default is `gemini-3.8-flash`; API version defaults to `v1beta`; fallback defaults are `gemini-3.5-flash-lite`, `gemini-flash-lite-latest`, and `gemini-flash-latest`. These names are environment-configurable and their availability is not verified by configuration alone. |
| Android API base | Debug uses `http://10.0.2.2:8080/api/v1/`; release uses the configured `API_BASE_URL` property/environment variable and otherwise defaults to `https://api.example.com/api/v1/` (placeholder). |

The configured Gemini model name and fallback names are configuration, not evidence that those models are enabled for a particular account. Key validity, model availability, network reachability, quota, and billing require live environment verification. This document does not inspect or reproduce `backend/.env` values. `backend/.env.example` names configuration variables only; no key or credential value is included here.

## 5. Functional modules and implementation status

| Module | Status | Current behavior and evidence |
|---|---|---|
| Identity and student profile | Implemented, with MFA setup limitation | Registration, password login, profile read/update, and JWT issuance are in `AuthController`, `AuthService`, `ProfileController`, and `ProfileService`. |
| Guardian consent | Implemented | Under-13 registration requires a different guardian email, creates a pending consent record, emails a time-limited link, and blocks study routes until a decision. See `AuthService`, `GuardianConsentService`, and `GuardianConsentEnforcementFilter`. |
| AI question answering | Implemented for text requests | `QuestionService.submitQuestion` persists a question, calls `GeminiAdapterService`, stores structured JSON answer and an AI-generation log, and returns the result. The Android Ask AI repository sends subject and text. |
| AI orchestration | Partially wired | `AiOrchestrator` implements safety checks, prompt templates, caching, quotas, usage logging, concurrency control, and in-flight request coalescing. Quiz generation uses it; the current question and alternative-explanation path calls `GeminiAdapterService` directly instead. |
| AI provider handling | Implemented | `GeminiAdapterService` constructs structured requests, reads response bytes through `RestClient.exchange()`, parses JSON, validates output, redacts diagnostic snippets, and retries transient unavailable responses across configured model fallbacks. |
| Alternative explanations | Backend implemented; Android API integration not found | `POST /questions/{id}/alternative-explanation` exists in `QuestionController`; no corresponding method is declared in Android `ApiService`. |
| Quiz generation and practice | Implemented | `QuizService` generates and owns quizzes, permits editing/deletion before attempts, starts attempts, grades answers server-side, and records results. AI generation uses `AiOrchestrator`; a static fallback quiz is used for selected AI failures. |
| Dashboard metrics | Implemented | `DashboardService` aggregates owned question counts, completed quiz counts, and averages of quiz-attempt scores by subject. It is not a predictive or adaptive-learning system. |
| Study history and bookmarks | Implemented online; local read cache | Backend history and bookmark routes are profile-scoped. Android Room caches successful question/history data and bookmark state. |
| Offline synchronization | Partially implemented | Android synchronizes/caches remote history, quiz results, dashboard data, and notification settings. The normal question-submission path still calls the network. A Room pending-bookmark table exists, but current app code does not invoke its queue/replay flow. |
| Image questions/uploads | Partially implemented, not connected to the active UI flow | Backend has image validation, upload records, storage abstractions, and a Gemini adapter accepting image bytes. However, the current `/uploads/authorize` controller calls a simpler `QuestionService` method, no binary-upload controller route is mapped, and `QuestionService` does not load/attach the optional image path. Android has an `ImagePreprocessor`, but `AskAiScreen` and `QuestionRepository` submit text only. |
| Notifications | Partially implemented across backend and Android | Backend schedules bookmark-review, repeated-mistake, and weekly-summary notification delivery, with preferences and device-token endpoints. Android also has local WorkManager notifications and preference UI; its current `ApiService` does not register/unregister device tokens with the backend. |
| Account privacy/deletion | Implemented | Account deletion requires password/TOTP reauthentication, marks deletion pending, retries storage/Redis cleanup, removes relational user data, and retains hashed audit/consent evidence according to configuration. See `AccountPrivacyService` and `AccountDeletionWorker`. |
| Answer reporting | Partially implemented | `AnswerReport` and its reason/status types are persisted, but no report submission or moderation controller endpoint was found. |
| Separate guardian/admin web portal | Not found in current application source | Guardian decisions use a public token-based browser page; there is no separate guardian login or general administration UI. |

**Status interpretation:** “Implemented” refers to a connected code path; “Partially implemented” refers to code present without a complete normal-user flow; “Planned/requirements-only” refers to requirements in `Core/MVP_SCOPE.md`, `Core/PRD.md`, or `Architecture_and_Engineering/IMPLEMENTATION_PLAN.md` without a connected implementation. Planned items are not described as working features below.

The planning documents describe text-and-image questions/CameraX upload and TOTP setup. Current source only provides a text Ask AI flow and no first-time TOTP secret-enrollment path. The MVP scope explicitly excludes public leaderboards, social networking, badges/gamification, voice/AR, microservices/API gateway, and offline AI generation; these are not current features.

### 5.1 Main application workflows

1. **Registration and login:** `AuthService.register` checks for duplicate email, derives whether the user is under 13 from date of birth, creates a user/profile, and creates a TOTP factor with an encrypted secret. Login checks the password and account status. When a TOTP factor is already verified, it issues an `MFA_SESSION` token; `/auth/mfa/verify` validates the code and then issues access/refresh tokens.
2. **Guardian consent:** The service stores a SHA-256 token hash with a 72-hour expiry. The email link GET renders a confirmation form without consuming the token; the form POST makes the decision. Approval activates the account and invalidates account-status cache; rejection suspends it. The verification page returns HTML directly.
3. **Ask AI:** The Android app validates that text is nonblank, submits `subject` and `questionText`, then displays the structured answer. The backend persists the question and answer. This route does not use `AiOrchestrator`, despite the separate quota endpoint and orchestration module.
4. **Quiz:** The backend generates a bounded number of questions using the profile’s education level, validates the generated output, and stores the quiz. If configured AI failures occur, it may use static fallback questions. Attempts are owned by the student and graded by the backend on submission.
5. **Offline study:** On successful API operations, the Android repository caches question, bookmark, quiz-result, dashboard, and preference data in account-scoped Room tables. WorkManager refreshes remote history when connectivity is available; local notifications evaluate cached study activity.
6. **Deletion:** The account service requires password and TOTP reauthentication, creates a durable deletion job, removes associated storage and Redis keys, deletes the user/profile graph, and writes a hashed audit record. A scheduled worker retries pending jobs.

## 6. User roles, account states, and permissions

| Principal/state | Source behavior |
|---|---|
| Student/application user | `UserPrincipal` grants `ROLE_USER`. Normal API routes require authentication; service/repository queries scope questions, quizzes, profiles, and attempts to the caller’s profile. |
| Under-13 student pending consent | `PENDING_CONSENT` is loaded from the live account-status service/cache. `GuardianConsentEnforcementFilter` blocks non-whitelisted study requests. |
| Suspended student | Study access is denied with `ACCOUNT_SUSPENDED`. Guardian rejection sets this state. |
| Guardian | No guardian user account or role is present. The guardian acts through the public, one-time consent token and does not receive student API access. |
| Monitoring principal | In dev/prod, Actuator has a separate HTTP Basic security chain requiring `ROLE_MONITORING`; credentials are configured from environment. |
| Development notification operator | Dev-only notification test/outbox routes require the configured development admin header; the main API security chain also requires authentication. |

There is no source-backed role hierarchy for teachers, schools, or a general application administrator. Registration creates an unverified TOTP factor, but current backend login issues full access/refresh tokens when the factor is not verified; no enrollment-secret/QR endpoint or enrollment screen was found. The Android MFA screen handles a login challenge only. This means the registration response’s `mfaRequired` flag should not be read as proof that initial MFA enrollment is completed or enforced.

## 7. Persistence model

All entities extending `BaseEntity` inherit `createdAt` and `updatedAt`. UUID identifiers are used for most student-owned records. The details below summarize current JPA entity properties and relationships; status/enumeration types are stored as application values rather than separate entities.

| Entity | Important fields | Relationships |
|---|---|---|
| `User` | `userId`, `email`, `passwordHash`, `dob`, `accountStatus`, `authVersion` | One-to-one `StudentProfile` and `GuardianConsent`; one-to-many `MfaFactor` and `MfaAttempt`. |
| `StudentProfile` | `profileId`, `educationLevel`, `learningStyle` | One-to-one `User`; owner of questions, quizzes/attempts, AI logs, bookmarks, notification settings/deliveries, device tokens, and answer reports. |
| `GuardianConsent` | `consentId`, `guardianEmail`, `status`, `tokenHash`, `tokenExpiresAt`, `resendCount`, `lastSentAt`, `approvedIp`, `verifiedAt` | One-to-one `User`. |
| `MfaFactor` | `factorId`, `type`, encrypted `secret`, `isVerified` | Many-to-one `User`. |
| `MfaAttempt` | `attemptId`, `success`, `ip`, `createdAt` | Many-to-one `User`. |
| `MfaRecoveryCode` | `recoveryCodeId`, `codeHash`, `createdAt` | Many-to-one `User`; no recovery-code controller flow was found. |
| `PasswordResetToken` | `resetTokenId`, `tokenHash`, `expiresAt`, `createdAt` | Many-to-one `User`. |
| `Question` | `questionId`, `subject`, `questionText`, `status` | Many-to-one `StudentProfile`; one-to-many `QuestionAttachment` and `Answer`. |
| `Answer` | `answerId`, `structuredJson`, `isAlternative` | Many-to-one `Question`; optional many-to-one `AiGenerationLog`. |
| `QuestionAttachment` | `attachmentId`, `firebaseStoragePath`, `mimeType`, `sizeBytes`, `sha256` | Many-to-one `Question`. |
| `QuestionBookmark` | `bookmarkId` | Many-to-one `StudentProfile` and `Question`; database enforces one bookmark per profile/question pair. |
| `ImageUpload` | `uploadId`, `storagePath`, `contentType`, `sizeBytes`, `expiresAt`, `attachedAt` | Many-to-one `StudentProfile`. |
| `QuestionSyncDeletion` | `deletionId`, scalar `profileId`, `questionId`, `deletedAt` | Stores sync-deletion identifiers; profile ID is scalar in this entity. |
| `AnswerReport` | `id`, `reason`, `comment`, `status` | Many-to-one `Answer` and `StudentProfile`. |
| `AiGenerationLog` | `logId`, scalar `questionId`/`quizId`, `modelId`, `promptVersion`, input/output tokens, first-token/total latency, `cacheHit`, `outcome` | Many-to-one `StudentProfile`. |
| `PracticeQuiz` | `quizId`, `subject`, `topic`, `status` | Many-to-one `StudentProfile`; one-to-many `QuizQuestion` and `QuizAttempt`. |
| `QuizQuestion` | `questionId`, `type`, `prompt`, `correctAnswer`, `explanation` | Many-to-one `PracticeQuiz`; one-to-many `QuizOption`. |
| `QuizOption` | `optionId`, `optionText`, `optionOrder` | Many-to-one `QuizQuestion`. |
| `QuizAttempt` | `attemptId`, `startTime`, `endTime`, `score` | Many-to-one `PracticeQuiz` and `StudentProfile`; one-to-many `QuizResponse`. |
| `QuizResponse` | `responseId`, `selectedOptionId`, `subjectiveText`, `isCorrect` | Many-to-one `QuizAttempt` and `QuizQuestion`. |
| `DeviceToken` | `tokenId`, `token`, `platform`, `appVersion`, `lastSeen` | Many-to-one `StudentProfile`. |
| `NotificationPreference` | `profileId`, bookmark/repeated-mistake/weekly-summary flags, quiet-hour times, `timezone` | One-to-one `StudentProfile`. |
| `NotificationDelivery` | `deliveryId`, `type`, `periodKey`, `payloadJson`, `status`, `createdAt`, `sentAt` | Many-to-one `StudentProfile`. |
| `AccountDeletionJob` | `userId`, `profileId`, `userIdHash`, `storagePathsJson`, `createdAt` | Durable deletion work record; identifiers are scalar. |
| `AccountDeletionAudit` | `userIdHash`, `deletedAt` | Hashed audit record; no relation to the deleted user row. |
| `GuardianConsentEvidence` | `userIdHash`, `consentStatus`, `consentAt`, `expiresAt` | Retained evidence keyed by hashed user ID. |

Flyway SQL is under `backend/src/main/resources/db/migration/` and `backend/src/main/resources/db/postgresql-migration/`; `ddl-auto` is `validate`, not schema generation. One migration creates `student_profile_subjects`, while the current `StudentProfile` entity has no corresponding collection/property. Treat that schema table as migration-level/legacy state unless manually reconciled. The Android Room database is separate from the backend schema and is version 5 (`StudyMentorDatabase`); its local entities include cached questions, bookmarks, quiz results, pending bookmark changes, sync cursor, dashboard metrics, notification preferences, and local notification deliveries.

## 8. HTTP API surface

All paths below are under `/api/v1` unless written as Actuator paths. Methods and paths come from current controller mappings. Normal API calls are stateless and authenticated unless noted otherwise; pending-consent and account-status filters further restrict the caller.

### 8.1 Authentication, consent, profile, and account

| Method and path | Access | Implementation |
|---|---|---|
| `POST /auth/register` | Public | Create student; may initiate guardian-consent email. |
| `POST /auth/login` | Public | Password check; returns MFA challenge only for an already-verified factor, otherwise returns access/refresh tokens. |
| `POST /auth/mfa/verify` | Public challenge | Verify TOTP against an `MFA_SESSION`; issue full tokens. |
| `POST /auth/refresh` | Public with refresh-token credential | Rotate refresh/access tokens. |
| `POST /auth/logout` | Permitted by security configuration | Revoke refresh token supplied in request body; no Bearer principal is used by controller. |
| `POST /auth/password/forgot` | Controller exists; currently requires authentication under the catch-all security rule | Requests reset email. The Android client calls this without a token, so the current security matcher prevents the intended public reset request. |
| `POST /auth/password/reset` | Controller exists; currently requires authentication under the catch-all security rule | Resets password using a one-time token; the Android client calls without a token, so the same access mismatch applies. |
| `GET /guardian-consent/verify?token=…&action=…` | Public | Renders HTML confirmation; GET does not consume token. |
| `POST /guardian-consent/verify` | Public | Applies approve/reject decision and returns HTML result. |
| `GET /guardian-consent/status` | Authenticated | Read the student’s consent status. |
| `POST /guardian-consent/resend` | Authenticated | Resend pending consent email subject to limits/cooldown. |
| `GET /profile/me` | Authenticated | Read current student profile. |
| `PATCH /profile/me` | Authenticated | Update optional education level/learning style fields. |
| `DELETE /account` | Authenticated; password/TOTP reauthentication required | Start or resume account deletion. |
| `POST /account/logout-all` | Authenticated; password/TOTP reauthentication required | Increment auth version and remove registered device tokens. |

### 8.2 AI, questions, and bookmarks

| Method and path | Access | Implementation |
|---|---|---|
| `GET /ai/quota` | Authenticated | Read remaining orchestrator quota. The current text-question route does not reserve this quota through `AiOrchestrator`. |
| `POST /uploads/authorize` | Authenticated | Current controller calls `QuestionService.authorizeUpload`, which returns a generated storage path/token rather than invoking the separate signed-upload service. |
| `POST /questions` | Authenticated | Submit text question and obtain structured Gemini answer. |
| `GET /questions?page=&size=` | Authenticated | Page current student’s question history. |
| `GET /questions/{id}` | Authenticated | Read an owned question and its primary answer. |
| `POST /questions/{id}/alternative-explanation` | Authenticated | Generate a backend alternative explanation. |
| `POST /questions/{id}/bookmark` | Authenticated | Create bookmark idempotently. |
| `DELETE /questions/{id}/bookmark` | Authenticated | Remove bookmark. |
| `GET /questions/bookmarks?page=&size=` | Authenticated | Page current student’s bookmarks. |

No mapped endpoint accepts image bytes or completes the `ImageUploadService` upload/attachment lifecycle. `QuestionCreateRequest` contains an optional image path, but the current `QuestionService` submission path does not use it.

### 8.3 Quizzes and dashboard

| Method and path | Access | Implementation |
|---|---|---|
| `POST /quizzes` | Authenticated | Generate and persist quiz. |
| `GET /quizzes?page=&size=` | Authenticated | Page owned quizzes. |
| `GET /quizzes/attempts` | Authenticated | List completed attempts. |
| `GET /quizzes/{id}` | Authenticated | Read owned quiz. |
| `GET /quizzes/{id}/edit` | Authenticated | Return owned quiz editing data. |
| `PUT /quizzes/{id}` | Authenticated | Update eligible quiz. |
| `DELETE /quizzes/{id}` | Authenticated | Delete owned quiz. |
| `POST /quizzes/{id}/attempts` | Authenticated | Start attempt. |
| `POST /quizzes/{id}/attempts/{attemptId}/submit` | Authenticated | Validate and grade submitted responses. |
| `GET /dashboard/metrics` | Authenticated | Return cached aggregate metrics. |

### 8.4 Notifications and development routes

| Method and path | Access | Implementation |
|---|---|---|
| `PUT /devices/token` | Authenticated | Register/update a device token. |
| `DELETE /devices/token` | Authenticated | Remove a device token. |
| `GET /notifications/preferences` | Authenticated | Read notification preferences. |
| `PUT /notifications/preferences` | Authenticated | Update notification preferences. |
| `POST /dev/notifications/test` | Dev profile, authenticated, plus development admin header | Enqueue/deliver a test notification. |
| `GET /dev/notifications/outbox` | Dev profile, authenticated, plus development admin header | Read local sender outbox when that sender is enabled. |

### 8.5 Operations endpoints

Actuator exposure is limited by configuration to health and metrics. In dev/prod it is on a loopback-bound management server (default port 8081) with a separate monitoring principal. Implemented operations include health/liveness/readiness and metrics endpoints; they are not student API routes.

## 9. Android pages, navigation, and client behavior

`MainAppScreen` hosts Navigation Compose. The bottom navigation has Ask AI, History, Quizzes, Dashboard, and Profile. Additional routes cover:

- Authentication routes: `LoginScreen`, `RegisterScreen`, and `GuardianConsentScreen`. Login handles an MFA challenge inline.
- Setup and study: `OnboardingScreen`, `AskAiScreen`, `AnswerDetailScreen`, and `QuestionHistoryDetailScreen`.
- Learning: `QuizzesScreen`, `DashboardScreen`, and `HistoryScreen`.
- Account settings: `ProfileScreen`, `NotificationSettingsScreen`, `PrivacyAccountScreen`, and `AboutScreen`.

The route list is in `ui/navigation/Screen.kt`; top-level flow wiring is in `ui/MainAppScreen.kt`. `MfaScreen`, `PasswordRecoveryScreen`, and `RecoveryCodesScreen` composables also exist, but the main navigation graph does not define dedicated routes for them; only the inline MFA challenge is connected. The app uses screen-level ViewModels and repositories, not server-rendered web pages. Notification deep links are handled in `ui/navigation/NotificationDeepLinks.kt`.

`ApiService` declares calls for authentication, profile, account actions, questions/bookmarks, quizzes, consent status/resend, dashboard, AI quota, and notification preferences. It does not declare the backend alternative-explanation, device-token, guardian browser verification, or upload routes. Retrofit error conversion (`ApiFailure`) extracts safe server codes/messages when possible and falls back to status-based messages; `NetworkViewModel` maps known codes to localized strings and offers retry for retryable failures.

Room is version 5 and uses account-scoped local rows:

| Room table/entity | Fields |
|---|---|
| `cached_questions` / `CachedQuestionEntity` | `accountId`, `questionId`, `subject`, `questionSnippet`, `hasImage`, optional `imageUrl`, `isBookmarked`, `status`, optional `createdAt`, `questionText`, optional `answerJson` |
| `cached_bookmarks` / `CachedBookmarkEntity` | `accountId`, `questionId`, `subject`, `questionSnippet`, `hasImage`, optional `imageUrl`, `status`, optional `createdAt`, optional `lastViewedAt` |
| `cached_quiz_results` / `CachedQuizResultEntity` | `accountId`, `attemptId`, `quizId`, `subject`, `score`, `totalQuestions`, `correctCount`, optional `submittedAt`, `detailsJson` |
| `pending_bookmark_changes` / `PendingBookmarkChangeEntity` | `accountId`, `questionId`, `isBookmarked` |
| `offline_sync_state` / `OfflineSyncStateEntity` | `accountId`, `cursor` |
| `cached_dashboard_metrics` / `CachedDashboardMetricsEntity` | `accountId`, `range`, `timezone`, `metricsJson`, `cachedAt` |
| `notification_preferences` / `NotificationPreferencesEntity` | `accountId`, bookmark/repeated-mistake/weekly-summary flags, optional quiet-hour start/end, `timezone`, `pendingUpload` |
| `local_notification_deliveries` / `LocalNotificationDeliveryEntity` | `accountId`, `type`, `periodKey`, `createdAt` |

`OfflineRepository.syncFromRemote` fetches remote history and quiz attempts for cache; `OfflineSyncCoordinator` and `OfflineSyncWorker` schedule refresh when connectivity is available. The Room pending-bookmark entity/DAO is not wired into the current normal bookmark submission flow. Ask AI is a text-only online request in the current UI/repository path.

## 10. Validation, errors, and security

### 10.1 Validation and error handling

- Controllers use `@Valid` on selected request DTOs; DTO constraints include required email/password/date, subject, and upload metadata. Additional service-level checks cover age/guardian requirements, MFA codes/rate limits, image size/type, quiz generation/edit/answer consistency, and ownership.
- `GlobalExceptionHandler` maps business errors, validation errors, unsupported media types, access denial, and unexpected exceptions to a common `ErrorResponse` shape containing status/error/code/message/timestamp/path (and optional details).
- Authentication and consent filters write their own structured error responses. The Android layer maps common backend error codes and HTTP statuses to localized messages instead of exposing arbitrary stack traces.
- Gemini output is parsed as JSON and validated against structured answer/quiz schemas. Provider failures are converted to business error codes; diagnostics include sanitized status/content-type/body snippets rather than the API key.

### 10.2 Authentication and authorization

- The API is stateless; CSRF is disabled and application API authorization uses Bearer JWTs. Passwords are BCrypt-hashed; TOTP secrets use AES-GCM encryption.
- JWT token types distinguish access, refresh, and MFA-session tokens. The authentication filter checks expiry/type and looks up live account status rather than trusting only a token claim.
- Redis-backed rate limiting covers authentication and AI-related routes, with an in-process fallback if Redis is unavailable.
- Guardian-consent verification GET/POST is public. CORS is narrowly customized for that exact path to accept local Mailpit origins (`localhost:8025`, `127.0.0.1:8025`) and the `null` sandbox origin. Other `/api/**` CORS origins come from configured exact origins; credentials are disabled.
- Normal API access is authenticated, with a notable configuration mismatch: password-recovery controller routes are not in the public `SecurityConfig` allowlist, while the Android client calls them without a Bearer token. The code therefore contains the functionality but the normal unauthenticated recovery flow is not currently reachable.
- Account deletion and logout-all require password plus TOTP reauthentication; deletion uses a resumable job and hashed audit/evidence records.
- `storage.rules` denies direct Firebase Storage access from clients. Backend storage gateways and signed-grant code exist, but the current upload controller path does not connect to that lifecycle.

## 11. Tests and their verified scope

The source inventory contains 37 backend test Java files, 8 Android JVM test files, and 12 Android instrumentation test files. This is an inventory of checked-in tests, not a claim that the suites were run as part of this documentation task.

| Test group | Examples and scope |
|---|---|
| Backend end-to-end/schema | `BackendEndToEndIntegrationTest` exercises HTTP-level student flows with Testcontainers MySQL and mocked mail/Gemini; `FlywaySchemaValidationIntegrationTest` validates migrations/JPA. Testcontainers tests are disabled when Docker is unavailable. |
| Backend AI | `GeminiAdapterServiceTest`, `AiOrchestratorCacheTest`, `AiQuotaServiceTest`, `AiResponseCacheTest`, `AiSafetyClassifierTest`, `PromptTemplateServiceTest`, `StructuredAnswerValidationTest`. |
| Backend identity/security | `AuthServiceGuardianConsentTest`, `GuardianConsentControllerTest`, `GuardianConsentServiceTest`, `GuardianConsentEnforcementFilterTest`, `SecurityRulesUnitTest`, `SecurityConfigCorsTest`, and rate-limit/config tests. |
| Backend feature services | Question upload/bookmark/storage, quiz grading, dashboard calculations/time windows, notification delivery/rules/scheduler, account-related utilities, and common exception/encryption tests. |
| Android JVM | `AuthViewModelTest`, `AskAiViewModelTest`, `ImagePreprocessorTest`, `QuizViewModelTest`, `DashboardViewModelTest`, `NotificationSettingsViewModelTest`, `NotificationDeepLinksTest`, and `LocalNotificationRulesTest`. |
| Android instrumented | Compose student flows, Ask AI, quiz, dashboard, privacy/account UI; Room DAO/migrations; offline account flow/pending bookmark work; Retrofit network contract tests. Requires Android test runtime/device. |

Test source paths are `backend/src/test/java/com/aistudymentor/`, `android/app/src/test/java/com/aistudymentor/`, and `android/app/src/androidTest/java/com/aistudymentor/`.

## 12. Incomplete features and technical limitations

1. **Initial MFA enrollment is incomplete.** Registration creates an unverified factor, but there is no source-backed API/UI to display its secret or enroll it. Login grants full tokens when the factor is unverified. Recovery-code persistence exists, but no recovery-code endpoints were found.
2. **Password recovery authorization is inconsistent.** Controller/service and Android client calls exist, but the Spring security allowlist does not permit unauthenticated forgot/reset calls.
3. **AI orchestration differs by feature.** Quiz generation uses the orchestrator’s cache/quota/safety/usage pipeline; text question and alternative-explanation requests call Gemini directly. The displayed quota is therefore not a reliable enforcement counter for those direct requests.
4. **Image-question workflow is disconnected.** The app’s active Ask AI screen is text-only; backend upload authorization controller does not use the signed-upload service, no binary upload mapping is present, and question submission does not attach the optional image path. Image upload/CameraX appears as a requirement in project planning documents, not a connected user flow.
5. **Some Android API features are missing.** The backend alternative-explanation and device-token routes have no corresponding methods in the current Retrofit interface. Recovery-code setup/management is also not wired to API endpoints.
6. **Recovery UI is not reachable in the navigation graph.** Password-recovery and recovery-code composables/ViewModel state exist, but there are no corresponding navigation routes; password recovery also has the backend authorization mismatch noted above.
7. **Offline capability is cache-first, not full offline authoring.** Remote data is cached for reading and notifications; question submission requires network. Pending bookmark data structures exist but are not connected to normal bookmark mutations or replay.
8. **Answer reporting is data-only.** Report entities/migrations exist without an API/controller or moderation workflow in current source.
9. **External deployment behavior is environment-dependent.** Gemini model support, provider key/quota/billing, Firebase credentials, production SMTP, and release signing require deployment-specific setup and manual verification. No live provider or production environment result is asserted here.
10. **Schema has migration-only state.** `student_profile_subjects` is created in SQL migrations but is not represented by a property in the current `StudentProfile` entity; inspect before relying on it in reports or new code.
11. **No standalone web or administrative application was found.** Current management access is Actuator-only and guarded separately; browser UI is limited to consent confirmation/result pages.

## 13. Source references

- Backend entry point: `backend/src/main/java/com/aistudymentor/BackendApplication.java`
- Backend dependencies/configuration: `backend/pom.xml`, `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-prod.yml`
- Security and errors: `backend/src/main/java/com/aistudymentor/config/SecurityConfig.java`, `JwtAuthenticationFilter.java`, `GuardianConsentEnforcementFilter.java`, `ApiRateLimitFilter.java`, `common/exception/GlobalExceptionHandler.java`
- Main backend modules: `backend/src/main/java/com/aistudymentor/{auth,profile,question,ai,quiz,dashboard,notification,account}/`
- Database: `backend/src/main/java/com/aistudymentor/*/entity/`, `backend/src/main/resources/db/migration/`, `backend/src/main/resources/db/postgresql-migration/`
- Android build/app entry: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`
- Android navigation/data: `android/app/src/main/java/com/aistudymentor/ui/`, `android/app/src/main/java/com/aistudymentor/data/`
- Storage policy: `storage.rules`
- Tests: `backend/src/test/java/`, `android/app/src/test/`, `android/app/src/androidTest/`

