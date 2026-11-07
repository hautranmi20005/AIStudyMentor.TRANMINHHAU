# API Specification v1.1

**Base Path**: `/api/v1`

## 1. Auth & Consent
- `POST /auth/register`: Returns user, prompts for consent if U13.
- `POST /auth/login`: Returns short-lived JWT or `MFA_REQUIRED` token.
- `POST /auth/mfa/verify`: Validates MFA and issues full Access/Refresh JWTs.
- `GET /users/me/guardian-consent`: Checks consent status. (403 Forbidden for Core APIs if pending).

## 2. Q&A
- `POST /questions`: Submits text/image path. Returns structured Gemini JSON.
- `GET /questions`: Paginated history.
- `POST /questions/{id}/bookmark`.

## 3. Quizzes & Dashboard
- `POST /quizzes`: Generates a new quiz via Gemini.
- `GET /quizzes/{id}`: Returns quiz prompt and options (WITHOUT answers).
- `POST /quizzes/{id}/attempts`: Starts attempt.
- `POST /quizzes/{id}/attempts/{attemptId}/submit`: Grades server-side, returns score.
- `GET /dashboard/metrics`: Returns total questions, quiz accuracy, subjects overview.
