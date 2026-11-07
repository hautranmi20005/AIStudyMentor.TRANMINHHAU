# Software Requirements Specification (SRS) v1.2

## Functional Requirements
* **FR-01A (Registration & Login)**: Email/Password registration with mandatory `dob`. Password max 72 bytes. Issue JWT tokens (15m Access, 7d Refresh with rotation and revocation) (Decision D4, D9).
* **FR-01B (Student Profile)**: Manage grade level, preferred subjects, and learning styles. Accessible even while Under-13 consent is pending.
* **FR-01C (Multi-Factor Authentication - MFA)**: TOTP (RFC 6238) authentication factor only (Decision D1). Rate-limited to max 5 failed attempts per 15 minutes, followed by a 15-minute lockout. Secret stored AES-GCM encrypted.
* **FR-01D (Under-13 Guardian Consent Workflow)**: Dynamic age evaluation at registration (<13 years old). Automatically sets `account_status` to `PENDING_CONSENT`. Dispatches 72-hour signed single-use verification token via email loop. Access to core study features blocked with HTTP 403 `GUARDIAN_CONSENT_REQUIRED` until verified (Decision D2). *(Note: legal review pending - not production-ready)*.
* **FR-02 (Text Question Submission)**: Submit academic text questions with subject metadata.
* **FR-03 (Photo Question Submission)**: Direct authenticated upload to private Firebase Storage bucket `users/{userId}/images/{imageId}`. Backend validates ownership before Gemini processing.
* **FR-04 (Structured AI Answer via Gemini)**: Generate step-by-step explanations conforming strictly to `docs/ai_response.schema.json`.
* **FR-05 (Alternative Explanations)**: Generate alternative pedagogical explanations or real-world analogies upon request (`POST /questions/{id}/alternative-explanation`).
* **FR-06A (Question History)**: Paginated history of previously submitted questions and answers.
* **FR-06B (Bookmarks)**: Bookmark and unbookmark questions via dedicated `question_bookmarks` table (Decision D7).
* **FR-07 (Practice Quiz Generation & Server-Side Scoring)**: Generate MCQ and Short-Answer quizzes via Gemini. Correct answers stored strictly on server in `quiz_questions.correct_answer` and never transmitted to client pre-submission. Server-side grading evaluates short-answers via normalized whitespace/case string comparison (Decision D8).
* **FR-08 (Progress Dashboard)**: Aggregate user metrics by subject, quiz count, and accuracy rate (`GET /dashboard/metrics`).

## Non-Functional Requirements
* **NFR-01 (Strict AI Schema Output)**: Gemini outputs must validate against `docs/ai_response.schema.json`. Malformed responses are rejected.
* **NFR-02 (Fast Exact-Match Cache)**: Redis exact-match cache (`qa:v1:{model}:{promptVersion}:{hash}`) must respond in <200ms.
* **NFR-03 (Native Android UI)**: Pure Jetpack Compose with Material 3. Clean architecture (MVVM, StateFlow, Room offline cache).
* **NFR-04 (Zero Secret Exposure)**: All API keys (Gemini, JWT, MFA encryption) must strictly reside in backend environment variables.
* **NFR-05 (Fail-Fast AI Resilience)**: If Gemini configuration is missing (`<SET_ME>`), AI calls fail fast with `AI_NOT_CONFIGURED` without crashing the application (Decision D3).
