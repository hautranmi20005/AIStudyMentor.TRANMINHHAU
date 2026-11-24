# Backend API Specification

This is the current endpoint inventory, checked against the Spring controllers. The runnable request collection is [`AIStudyMentor.http`](./requests/AIStudyMentor.http); set `studentEmail`, `studentPassword`, `guardianEmail`, and `totpCode` in a private REST Client environment. Token and resource identifiers are captured from earlier responses.

The older [Architecture and Engineering API document](../Architecture_and_Engineering/API_SPEC.md) predates the implemented routes. This file and the request collection are authoritative for the current backend.

## Conventions

- Application base URL: `http://localhost:8080/api/v1`.
- All API requests should send `X-Correlation-Id`; the server validates or replaces it and returns the effective value in the same response header.
- Protected routes require `Authorization: Bearer {{accessToken}}`.
- Bodies use JSON except `GET /guardian-consent/verify`, which returns an inert HTML confirmation page.
- `400`, `401`, `403`, `404`, `409`, `429`, AI/provider failures, and unexpected errors use `{ "code", "message", "details"?, "correlationId", "timestamp" }`. Validation details contain field names and messages, never rejected input. `429` responses include `Retry-After`.
- HTTP request examples are named by endpoint below. Use them in order where a request depends on a token or ID captured by a preceding request.

## Authentication

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `POST /auth/register` | Public | `registerStudent` | Creates an account/profile and an unverified TOTP factor. Returns a short-lived setup token, secret, and provisioning URI; under-13 registration also starts guardian consent. |
| `POST /auth/login` | Public | `loginStudent` | Checks credentials and returns an MFA challenge when enrolled, or a setup challenge if enrollment is incomplete. |
| `POST /auth/mfa/verify` | Public, requires an MFA session or setup token | `verifyTotp` | Verifies TOTP enrollment or a login challenge and returns access/refresh tokens. Login challenges may use a one-time recovery code. |
| `POST /auth/refresh` | Public, requires refresh token | `refreshTokens` | Rotates tokens and revokes the old refresh token. |
| `POST /auth/logout` | Authenticated | `logout` | Revokes the submitted refresh token; returns `204`. |
| `POST /auth/password/forgot` | Public | `requestPasswordReset` | Returns `202` regardless of whether the email is registered; registered users receive a single-use reset link. |
| `POST /auth/password/reset` | Public | `resetPassword` | Consumes the reset token, changes the password, and invalidates existing sessions. MFA remains required at the next login. |

MFA recovery-code regeneration and MFA disable routes are not present in the current `AuthController` or other controller mappings.

## Account Privacy

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `DELETE /account` | Authenticated | `{ "password": "...", "code": "123456" }` | Requires the current password and verified TOTP. Returns `204` after deletion. Cleanup is resumable across database, image-storage, and Redis failures; repeat the request to resume an interrupted deletion. |
| `POST /account/logout-all` | Authenticated | `{ "password": "...", "code": "123456" }` | Requires password and TOTP, invalidates existing sessions through the account auth version, removes registered device tokens, and returns `204`. |

## Guardian Consent

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `GET /guardian-consent/verify?token=…&action=approve\|reject` | Public | `renderGuardianConsentConfirmation` | Renders confirmation HTML; GET does not consume the token. Obtain the token from the local Mailpit email. |
| `POST /guardian-consent/verify?token=…&action=approve\|reject` | Public | `submitGuardianConsentDecision` | Consumes the single-use consent token and updates consent/account state. |
| `GET /guardian-consent/status` | Authenticated | `getGuardianConsentStatus` | Returns the current user's consent state. |
| `POST /guardian-consent/resend` | Authenticated | `resendGuardianConsentEmail` | Requests a consent email resend; returns `202`. |

## Profile

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `GET /profile/me` | Authenticated | `getMyProfile` | Returns the authenticated user's profile. |
| `PATCH /profile/me` | Authenticated | `updateMyProfile` | Updates the `educationLevel` and `learningStyle` fields accepted by the current request DTO. |

## Questions, Upload Authorization, and Bookmarks

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `POST /uploads/authorize` | Authenticated | `authorizeImageUpload` | Returns a generated storage path and opaque token. `QuestionService` currently does not validate or consume that token, and no controller accepts upload bytes; this does not complete an image-upload flow. |
| `GET /ai/quota` | Authenticated | — | Returns remaining per-user daily AI calls and tokens. |
| `POST /questions` | Authenticated; guardian-consent rules apply | `submitQuestion` | Persists a text question. When Gemini is configured, `QuestionService` calls `GeminiAdapterService`; if invoked without a configured key, it returns a deterministic local smart-solver response. The dev startup guard normally prevents startup without a key. Provider timeouts return `504 AI_PROVIDER_TIMEOUT`; other provider transport/HTTP failures return `502 AI_PROVIDER_ERROR`. The outbound Gemini client uses a 10-second connect and 60-second response timeout. The `imageStoragePath` request field is not consumed. The route bypasses `AiOrchestrator`, so its cache, quota reservation, and safety pipeline are not applied here. |
| `GET /questions?page=0&size=20` | Authenticated | `listQuestions` | Returns paged history scoped to the current user. The controller accepts only `page` and `size`; search, date, bookmark, status, and sort filters are not implemented. |
| `GET /questions/{id}` | Authenticated | `getQuestion` | Returns a question only when it belongs to the current user. |
| `POST /questions/{id}/alternative-explanation` | Authenticated | `requestAlternativeExplanation` | Requests an alternative explanation on the backend. The Android `ApiService` has no corresponding call or UI action. |
| `POST /questions/{id}/bookmark` | Authenticated | `setBookmark(..., true)` | Adds a bookmark; repeating the request leaves it bookmarked. |
| `DELETE /questions/{id}/bookmark` | Authenticated | `setBookmark(..., false)` | Removes a bookmark; deleting an already-absent bookmark succeeds and leaves it absent. |
| `GET /questions/bookmarks?page=0&size=20` | Authenticated | `listBookmarks` | Returns the current user's bookmarked questions. |

The history screen reads its locally cached questions and bookmarks. There is no remote history search endpoint, question-delete endpoint, or `/sync/history` controller in this checkout. Flyway V9 creates some supporting database indexes and the deletion-tombstone table, but those objects are not exposed through these routes.

### AI provider behavior and current wiring

- `AiOrchestrator` contains provider selection, request validation, safety checks, Redis response caching, quotas, concurrency control, and telemetry. The current `QuestionService` does not call it: question submission calls `GeminiAdapterService` directly when configured and uses its local deterministic smart-solver fallback when not configured. Do not infer cache hits, quota enforcement, or provider-only answers from a successful `POST /questions` response.
- `AiResponseCache` and cache-key generation exist, but the normal question-submission route does not use them. A repeated question is therefore not a verified cache hit.
- `GET /ai/quota` exposes the quota service's remaining counters; the current text-question route does not reserve or reconcile those counters through `AiOrchestrator`.
- The alternative-explanation route explicitly validates Gemini configuration before generating an answer. Its availability is backend-only; the Android client does not expose it.
- Android Retrofit requests allow up to 90 seconds to read the backend response (95-second total call timeout) so the app does not abandon a normal AI response before the backend's provider timeout. `AI_PROVIDER_TIMEOUT`, `AI_PROVIDER_ERROR`, and `AI_NOT_CONFIGURED` map to distinct friendly messages; background quota-refresh errors do not display as question-submission failures.
- The `AiOrchestrator` tests do not establish that the normal question route uses that pipeline. Provider behavior and the local fallback should be reconciled before treating AI acceptance claims as complete.

## Quizzes

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `POST /quizzes` | Authenticated | `{ "subject": "Mathematics", "numQuestions": 5 }` (1–10 questions) | Generates questions through the configured AI provider, applies the AI quota/safety pipeline, and persists the resulting quiz. |
| `GET /quizzes?page=0&size=20` | Authenticated | `listQuizzes` | Returns the current user's paginated quizzes. |
| `GET /quizzes/attempts` | Authenticated | `listCompletedQuizAttempts` | Returns the current user's completed attempt history. |
| `GET /quizzes/{id}` | Authenticated | `getQuiz` | Returns quiz details; correct answers are omitted before submission. |
| `PUT /quizzes/{id}` | Authenticated | `{ "subject": "Mathematics" }` | Updates the quiz subject only while the quiz has not been started. |
| `DELETE /quizzes/{id}` | Authenticated | No body | Deletes a quiz only while it has not been started; started quizzes and their attempt history are protected (`409 QUIZ_ALREADY_STARTED`). |
| `POST /quizzes/{id}/attempts` | Authenticated | `startQuizAttempt` | Creates a new attempt (`201 Created`). |
| `POST /quizzes/{id}/attempts/{attemptId}/submit` | Authenticated | `submitQuizAttempt` | Grades submitted responses on the server and returns results. |

Quiz DTOs omit correct answers before submission. The service validates question/option ownership, rejects duplicate or foreign question responses, and computes the score on the server. The Android client exposes AI generation, list/start/submit, subject editing and deletion for quizzes that have not been started, plus attempt-history calls. AI generation requires an onboarded profile and a working configured provider; provider failures are returned rather than replaced with placeholder content.

## Notifications

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `PUT /devices/token` | Authenticated | `{ "token": "<FCM token>", "platform": "ANDROID", "appVersion": "1.0" }` | Registers or refreshes the caller's device token and last-seen time. A globally unique token is reassigned to the authenticated profile when ownership changes. |
| `DELETE /devices/token` | Authenticated | `{ "token": "<FCM token>" }` | Removes the matching device token only from the caller's profile. Returns `204`; repeated deletion is safe. |
| `GET /notifications/preferences` | Authenticated | — | Returns per-type enabled flags, optional paired quiet-hour boundaries, and an IANA timezone. Missing preferences default to all types enabled, no quiet hours, and UTC. |
| `PUT /notifications/preferences` | Authenticated | `{ "bookmarkReviewEnabled": true, "repeatedMistakeEnabled": true, "weeklySummaryEnabled": false, "quietHoursStart": "22:00:00", "quietHoursEnd": "07:00:00", "timezone": "America/Los_Angeles" }` | Replaces the caller's notification preferences. Quiet hours must be both present or both absent; invalid IANA zones return `400`. |
| `POST /dev/notifications/test` | Authenticated, `dev` profile only, requires `X-Dev-Admin-Key` | `{ "type": "BOOKMARK_REVIEW" }` | Queues a generic test notification for the caller and returns `202 Accepted`. Not registered outside `dev`. |
| `GET /dev/notifications/outbox` | Authenticated, `dev` profile only, requires `X-Dev-Admin-Key` | — | Returns intended generic payloads when `APP_PUSH=local`; returns `404` when the FCM sender is selected. |

`APP_PUSH=local` stores intended payloads in the development outbox without contacting FCM. `APP_PUSH=fcm` uses Firebase Admin credentials from `FIREBASE_SERVICE_ACCOUNT_FILE` and `FIREBASE_PROJECT_ID`; only FCM's explicit `UNREGISTERED` response causes a token to be removed. Production defaults to FCM and fails startup if credentials are unavailable.

The hourly scheduler sends one notification per profile/type/ISO week, enforced by a database uniqueness constraint. Bookmark review requires a bookmark not viewed for at least 3 days (opening its detail updates `last_viewed_at`). Repeated-mistake alerts require incorrect answers on at least two distinct attempts for the same subject/topic in the preceding 30 days. Weekly summaries require activity in the preceding 7 days and are scheduled in the user's local Monday/Tuesday daytime window. Preferences and quiet hours are evaluated in the stored timezone. Suspended/pending-consent accounts are skipped; under-13 accounts require approved guardian consent. Payloads contain generic text and only a deep-link route, never question or topic content.

Android ships in **Mode B** when no Firebase project is configured: it evaluates synced Room history with account-scoped periodic WorkManager work and posts local notifications on per-type channels. It asks for `POST_NOTIFICATIONS` permission from Profile & Settings and links taps to Bookmarks, Quizzes, or Dashboard. Preferences are stored locally while offline and synchronized through the endpoints above. **FCM delivery was not exercised in this mode.**

## Dashboard

| Method and path | Access | Request/example | Behavior |
|---|---|---|---|
| `GET /dashboard/metrics` | Authenticated | `getDashboardMetrics` | Returns total questions, completed quizzes, average quiz-attempt score, and per-subject question/quiz counts plus average attempt score. The controller accepts no range or timezone parameters. `DashboardService` is annotated with a per-user Spring cache. |

The dashboard formulas are implemented in `DashboardService`; they average persisted quiz-attempt scores and are not weighted per-question accuracy or time-windowed trends.

The full metric definitions, storage-timezone assumption for legacy `DATETIME(6)` columns, aggregation source tables, bucket policy, and deterministic insight rules are in [`DASHBOARD_METRICS.md`](./DASHBOARD_METRICS.md).

For seeded-user validation, [`scripts/seed-dashboard.ps1`](../scripts/seed-dashboard.ps1) inserts varied question and quiz activity for an existing profile. [`scripts/verify-dashboard.ps1`](../scripts/verify-dashboard.ps1) compares the authenticated API response with independent SQL aggregates. Supply `DASHBOARD_ACCESS_TOKEN`, `DB_PASSWORD`, the profile UUID, and the backend `StorageTimeZone`; neither script prints credentials.

## Management API

Management endpoints bind to `127.0.0.1:8081`, separate from the application port. In `dev` and `prod`, use configured Basic Auth credentials. The health endpoint reports MySQL and Redis readiness plus a Gemini configuration flag; the Gemini check does not call the provider.

| Method and path | Request/example | Behavior |
|---|---|---|
| `GET /actuator/health` | `managementHealth` | Overall local health. |
| `GET /actuator/health/liveness` | `managementLiveness` | Process liveness. |
| `GET /actuator/health/readiness` | `managementReadiness` | Readiness includes application readiness state, MySQL, and Redis. |
| `GET /actuator/metrics` | `managementMetricNames` | Lists registered metric names. |
| `GET /actuator/metrics/{name}` | `managementMetricDetail` | Reads an individual metric; example uses `ai.calls`. Other P04 meters include `ai.errors`, `ai.latency`, `cache.hit`, `cache.miss`, `ai.tokens.in`, and `ai.tokens.out`. |

## Implemented storage without a complete client flow

`answer_reports` and `image_uploads` database entities/migrations exist, and image-storage service classes exist. There is no answer-report controller, and the Android API has no report call. The question controller exposes upload authorization only; it has no upload-content route, and the Android API has no image-upload or image-question method. These are not end-to-end features in the current client.

The account-deletion controller is present at `DELETE /account`; it requires password and MFA reauthentication and runs a resumable deletion job. The Android API and privacy screen expose the request. No live P19 account-deletion journey was completed.

Other routes not present in the current controllers include MFA recovery-code regeneration/disable, question search/filter parameters, question deletion, `/sync/history`, quiz suggested-topic, quiz-attempt detail, upload-byte transfer, and answer reporting.
