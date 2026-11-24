# P19 Emulator Demo Script

**Planned duration:** 11 minutes

**Audience:** peer/student reviewers

**Execution status:** Bootstrap and backend readiness were verified after correcting the local environment configuration. The user walkthrough was not executed end-to-end; the remaining steps are a plan with expected results, not evidence that those features passed. See the [startup evidence](./evidence/final/backend-startup.md) and [acceptance checklist](./evidence/final/ACCEPTANCE_CHECKLIST.md).

## Preparation

- Use Temurin JDK 17.0.21, Docker Compose, and a booted `emulator-5554`.
- Keep the Gemini key in ignored `backend/.env`; never print or paste it into logs, screenshots, or chat.
- Run `.\scripts\demo-up.ps1` on Windows or `bash ./scripts/demo-up.sh` to launch with the configured Gemini provider. Ask AI sends academic questions to Gemini; a valid `GEMINI_API_KEY` is required. There is no local answer/demo mode.
- The standard `bash ./scripts/demo-up.sh` path uses the configured Gemini provider. The expected ready state is healthy MySQL, Redis, Mailpit, Spring Boot, and the installed debug APK.
- Current seeded credentials, available only in the local dev profile: `demo.middle@local.test` / `DemoPass123!`. Generate TOTP with `.\scripts\totp.ps1` or `bash ./scripts/totp.sh`. Under-13 consent mail is viewed in Mailpit at `http://127.0.0.1:8025`.
- Use a throwaway adult account for account deletion. Do not delete the reusable seeded accounts.

## Walkthrough agenda

| Time | Task | Expected result | Current implementation / P19 evidence |
|---|---|---|---|
| 00:00–00:30 | Launch the app and confirm backend health | App reaches the sign-in screen; management health is `UP` | **Verified:** `demo-up.ps1` completed; the app launched and authenticated Actuator health returned `UP`. No user journey was performed. |
| 00:30–01:25 | Register an adult and complete MFA | Registration creates the user, TOTP is enrolled, and a valid code opens an authenticated session | Android/backend paths and automated tests exist; no P19 live run. |
| 01:25–01:55 | Complete onboarding | Save education level and learning style; return to Ask | Profile endpoints and UI exist; no P19 live run. |
| 01:55–02:40 | Ask a text question | Receive and open a structured explanation | The active question path calls Gemini directly and returns a clear provider/configuration error rather than synthesizing a local answer; it bypasses the AI orchestrator. |
| 02:40–03:10 | Repeat the exact same question | Expected: response is served from cache without a second provider call | **Not currently available on the active path:** `QuestionService` does not use `AiResponseCache`; no cache-hit claim is valid. |
| 03:10–03:40 | Request an alternative explanation | A distinct explanation appears on the same question | Backend route exists, but Android exposes no API call or UI action; not executable in the app. |
| 03:40–04:20 | Ask a photo question | Select an image, upload it, and receive an image-aware answer | **Not implemented end-to-end:** no upload-byte controller or Android image-question API is present. |
| 04:20–05:00 | Search history and bookmark an item | Search narrows history; bookmark is visible in the Bookmarks tab | Bookmark detail and idempotent add/remove APIs exist; history search remains absent and the P19 live journey was not run. |
| 05:00–05:30 | Enable airplane mode and open saved history | Previously synchronized content remains readable; reconnect resumes sync | Room-backed offline code and emulator tests exist; no P19 airplane-mode journey was run. |
| 05:30–06:30 | Generate a quiz, submit answers, and review | The configured AI provider generates topic/level-relevant questions; answer keys stay hidden until server-side scoring | Quiz generation now uses `AiOrchestrator`, validates generated content, and the Android UI supports generation plus pre-attempt subject edit/delete. Automated coverage uses a mocked provider; **a live Gemini request and P19 walkthrough remain unverified**. Provider failure should be shown as an error, not replaced with static quiz content. |
| 06:30–07:00 | Open Progress | Dashboard shows saved question/quiz totals and scores | Basic aggregate endpoint exists; it is not the documented time-window analytics. No P19 live run. |
| 07:00–07:30 | Open notification settings and trigger a notification | Reminder preferences are visible; a scheduled local notification opens the intended destination | Profile settings are wired to WorkManager, but local notification delivery was not verified in P19. FCM delivery is not verified. |
| 07:30–08:30 | Register an under-13 account and inspect guardian consent | Account is pending; guardian message is visible in Mailpit; restricted actions remain blocked until approval | Backend consent paths have automated coverage; no P19 live run and no legal-compliance determination. |
| 08:30–09:00 | Report an answer | A report is accepted and can be found by a reviewer | **Not implemented in current API/client:** a database table exists, but there is no report controller or Android action. |
| 09:00–09:40 | Reauthenticate and delete the throwaway account | Deletion succeeds; the deleted account cannot access protected resources | Backend deletion job and Android privacy flow exist; automated evidence is historical, not a P19 live run. |
| 09:40–11:00 | Demonstrate controlled failures | Show a Redis outage, a provider-auth failure, and a rate limit without exposing credentials | **Not executed in P19.** Only attempt after a healthy backend start and with a disposable account. |

## Failure-demo safety notes

These are planned procedures, not executed results:

1. **Redis unavailable:** stop only the dev Compose Redis service, observe readiness/API behavior, then start it again. Do not remove containers or volumes. The exact user-facing error must be captured before presenting this as a verified failure demonstration.

   ```powershell
   docker compose --project-name aistudymentor-dev --env-file backend/.env -f docker-compose.dev.yml stop redis
   docker compose --project-name aistudymentor-dev --env-file backend/.env -f docker-compose.dev.yml start redis
   ```
   ```bash
   docker compose --project-name aistudymentor-dev --env-file backend/.env -f docker-compose.dev.yml stop redis
   docker compose --project-name aistudymentor-dev --env-file backend/.env -f docker-compose.dev.yml start redis
   ```

2. **Wrong Gemini key:** use a disposable child process with a deliberately invalid value supplied only through that process's environment. Do not edit `backend/.env`, echo the value, or save it in logs. Confirm that the API returns a standardized error without provider response content before presenting the result.

   PowerShell example after loading the local development environment: dot-source `scripts/dev-common.ps1`, call `Import-DevEnvironment`, set `$env:GEMINI_API_KEY` to a non-secret invalid demo marker in that shell only, and start the backend child process. Close that shell after the test; never put the marker or a real key in an application log or checked-in file.

3. **Rate limit:** use a disposable account and a bounded burst of invalid login attempts within the configured window. Expect a `429` response and `Retry-After`; stop after the documented limit is reached.

## Evidence discipline

Capture screenshots or `adb shell screenrecord` clips only while actually performing a step. Name each artifact after the demonstrated behavior and record failures/skips in the [P19 acceptance checklist](./evidence/final/ACCEPTANCE_CHECKLIST.md). P08/P18 screenshots and automated tests are historical evidence and must not be relabeled as P19 walkthrough results.
