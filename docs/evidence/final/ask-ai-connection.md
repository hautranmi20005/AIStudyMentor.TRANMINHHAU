# Ask AI connection diagnosis

## Observed evidence

- The backend reported Gemini as configured (`configured=true`); the key value was not read or printed. This confirms presence, not provider acceptance.
- The log contained two `Gemini API request failed (ResourceAccessException)` warnings at 23:58:57 and 00:00:46 on 2026-11-17/18. The previous adapter log format omitted the nested cause, so those entries cannot establish timeout versus DNS/TLS/socket failure.
- The backend later logged two completed `QuestionService.submitQuestion` requests at 00:04. Those requests reached the authenticated controller/service; an expired or rejected bearer token is not the cause for those requests.
- Host DNS resolution and TCP 443 connectivity to `generativelanguage.googleapis.com` succeeded during diagnosis. This does not verify Gemini API-key validity or provider response.
- The question JSON contract aligns: Android sends `subject` and `questionText`; the backend DTO accepts both. The emulator's `10.0.2.2:8080` route reaches the server.

The connection snackbar is the Android `IOException` fallback. Before this change, OkHttp's default read timeout was 10 seconds, while the app waits synchronously for the backend's Gemini call. The Android request could therefore time out while the server continued handling the request. A failing quota refresh could also emit the same global snackbar, obscuring the source.

## Changes and verification

- Backend outbound AI HTTP now has a 10-second connect timeout and a 60-second response timeout. Socket/HTTP timeouts are returned as `504 AI_PROVIDER_TIMEOUT`; non-timeout transport or provider HTTP failures remain sanitized `502 AI_PROVIDER_ERROR`. Logs record only the root exception class, never the URL query, key, request, or provider body.
- Android Retrofit uses 15-second connect, 90-second read, and 95-second overall call timeouts, leaving room for the backend to return its bounded provider error.
- Android maps provider timeout/unavailable/unconfigured responses to distinct localized messages. A secondary quota refresh failure is reflected in the quota status rather than shown as a question-submission network error.
- Focused Maven tests: `GeminiAdapterServiceTest` — 6 passed, 0 failures/errors/skips.
- Focused Android tests: `AskAiViewModelTest` — 6 passed, 0 failures/errors/skips.
- After restarting with the code changes, `demo-up.ps1` completed. `dev-status.ps1` reported MySQL, Redis, Mailpit, and Actuator healthy; Actuator health was `UP`. The updated APK was built, installed, and launched on `emulator-5554`.

No live Gemini generation was issued during validation, so the real provider key's validity and a successful paid/provider response remain unverified. Run a single text question from the emulator to exercise the provider path; if it fails, the new backend log distinguishes `AI_PROVIDER_TIMEOUT` from other transport failures without exposing credentials.
