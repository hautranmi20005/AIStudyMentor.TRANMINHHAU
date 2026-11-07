# Test Strategy v1.1

## 1. Security & Compliance Tests
- **MFA Bypass**: Attempt to access `/questions` using pre-MFA token (Expected: 403).
- **Consent Bypass**: Attempt Q&A as unverified U13 user (Expected: 403).
- **Brute Force**: Exceed MFA retry limits.
- **Quiz Key Leakage**: Inspect `/quizzes/{id}` JSON response to ensure `correct_answer` is absent.

## 2. Core Functional Tests
- End-to-end Text Question -> Gemini -> Cache -> Response.
- Firebase Image upload -> Backend ownership validation -> Gemini OCR -> Response.
- Redis offline -> Fallback to direct Gemini call.

## 3. Android UI Tests
- Jetpack Compose loading/error states.
- Offline navigation to cached history.
