# P19 Emulator Acceptance Checklist

**Status:** Not accepted; release-candidate gate is blocked.

**Checklist basis:** The requested Blueprint Section 19 source was not found in the workspace. This checklist maps the P19 requirements supplied in the task and does not invent additional blueprint criteria.

## Environment and build gates

| Gate | Result | Evidence / notes |
|---|---|---|
| Branch | Verified | `feature/p19-final`, created from `2579e16`; existing dirty changes were preserved. |
| Required JDK | Verified | Temurin 17.0.21 selected for Maven and Gradle. |
| Local dependencies | Verified | MySQL, Redis, and Mailpit containers reported healthy; Redis returned `PONG`. No volumes were reset. |
| Flyway migration state | Verified | The local MySQL history reports successful V1–V16 after the failed startup. See [observed rows](./flyway-history.txt). |
| Emulator | Verified | `emulator-5554` reachable; Android 17, 1080×2400. |
| Demo script syntax | Verified | PowerShell and Bash syntax checks passed. |
| Demo bootstrap | **Passed after local config correction** | Diagnosed and fixed the malformed/mismatched datasource settings and stale Spring property overrides in the local `.env` without changing secrets. `.\scripts\demo-up.ps1` completed: actuator `UP`, Gradle debug build successful, APK installed and launched. See [startup evidence](./backend-startup.md). |
| Backend package | Passed with tests skipped | `mvnw --batch-mode --no-transfer-progress -DskipTests package` exited 0 under JDK 17. This is a package build, not a test result. |
| Backend `verify` | **Incomplete; not passed** | The JDK 17 run was interrupted after approximately 13 minutes while `BackendEndToEndIntegrationTest` was executing. Nine fresh Surefire XML files record 29 completed tests, 0 failures, 0 errors, and 0 skips; the integration class has no fresh report and final Maven status is unavailable. See [partial Maven status and XML](./backend/MAVEN-VERIFY-STATUS.md). |
| Android build/tests/lint | Verified with latest targeted rerun | Required JDK 17 and Gradle compiler flag used. Latest JVM unit XML totals: 21 passed, 0 failures/errors/skips; latest targeted connected account-flow XML: 2 passed, 0 failures/errors/skips ([report](<../../../android/app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17-_app-.xml>)). The earlier full connected run recorded 18 passed. `lintDebug` exited 0 with 0 `HardcodedText` findings (42 total lint findings). |
| Debug APK | Built, installed, and launched | [P19 debug APK](./artifacts/AIStudyMentor-debug-p19.apk), SHA-256 `83B7CC23898D6D672EF41897AE1039AEEA81A69D7F8D29EF86050FBA4B916D85`. The successful bootstrap ran `adb install -r` and launched the app on `emulator-5554`. |
| Emulator app launch | Verified, launch screen only | [Sign-in screen capture](./screenshots/debug-app-launch-only.png) shows `MainActivity` on `emulator-5554`; no authentication/backend scenario is shown. |
| JaCoCo | Not configured / no P19 report | No coverage snapshot was generated. |

## Partial local walkthrough (2026-11-18)

The opt-in local demo mode used the stub provider with the Gemini key cleared. It did not contact Gemini and does not establish real-provider behavior. These results are partial evidence only; they do not make the release gate green.

| Step | Result | Evidence / notes |
|---|---|---|
| Local environment and app startup | Passed | Historical run before demo-mode removal: `.\scripts\demo-up.ps1 -LocalAiDemo` started local services, backend, and Android app. |
| Adult registration/sign-in and profile update | Partial | Registration/sign-in and onboarding API update succeeded; new-account MFA enrollment is blocked by the missing TOTP enrollment/activation endpoint. A seeded-account MFA challenge/verification succeeded once; later verification attempts were rate-limited (`429 RATE_LIMIT_EXCEEDED`). |
| Ask text | Passed, local stub only | Android returned `1 + 1 = 2` with the visible demo label. [Answer screenshot](../live-walkthrough-20261118/07-local-demo-answer.png) |
| Bookmark | Passed | Android bookmark action succeeded and the detail UI reflected it. [Bookmarked answer](../live-walkthrough-20261118/08-bookmarked-answer.png) |
| History list | Failed before fix; code-level regression test passed | The screen was empty after refresh despite server history. Session-reactive Room flows now address the captured-null-account cause; connected test passed 2/2 ([XML report](<../../../android/app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17-_app-.xml>)), but the live screen has not been rerun. [Before-fix screen](../live-walkthrough-20261118/10-history-after-refresh.png) |
| Quiz and dashboard APIs | Passed, local stub only | API quiz generation/edit/attempt/server grading/deletion and dashboard read succeeded. Not a complete Android Quiz journey or real-AI test. |
| Under-13 consent email | Partial | Registration reached `PENDING_CONSENT`; guardian message was visible in Mailpit. Parent confirmation was not completed. |
| Notification preferences | Failed | PUT returned HTTP 500 with Hibernate `AssertionFailure: null identifier (NotificationPreference)`; unresolved. |
| Photo, airplane-mode reading, WorkManager delivery, answer report, account deletion | Not verified | No success evidence was captured. |

## User journeys and acceptance

| Requirement | Result | Verification |
|---|---|---|
| Adult registration → MFA → onboarding | Partially verified live | Registration/sign-in and profile update passed in the local walkthrough; new-account MFA enrollment is blocked by the missing enrollment/activation endpoint. |
| Ask text | Verified in local demo mode only | Local stub answered `1 + 1 = 2` and Android visibly marked it as a demo. This is not a Gemini result. |
| Repeat question/cache hit | Not implemented on active path | `QuestionService` does not use `AiResponseCache`; no cache-hit evidence. |
| Alternative explanation | Backend only | Route exists; Android API/UI action is absent. |
| Photo question | Not implemented end-to-end | No upload-byte controller or Android image-question API. |
| Bookmark | Verified in local demo mode | Android add action and saved-answer detail passed; [screenshot](../live-walkthrough-20261118/08-bookmarked-answer.png). |
| History search | Not implemented; History refresh initially failed | There is no search endpoint/UI. The account-scoped flow issue is patched and its connected regression test passes, but the live UI has not been rerun. |
| Offline reading | Implemented but not verified live | Room/offline code and connected tests exist; no airplane-mode journey was completed. |
| Quiz generation/submit/review | API partially verified with local stub | API generation/edit/attempt/server grading/deletion succeeded; no full Android quiz journey or real-AI verification. |
| Dashboard | API partially verified | Dashboard API read succeeded; no complete Android dashboard journey or time-window analytics. |
| Notification | Failed on preference update | The observed PUT returned HTTP 500 from Hibernate `AssertionFailure: null identifier (NotificationPreference)`. WorkManager delivery and FCM remain unverified. |
| Under-13 consent | Partially verified live | Registration reached pending consent and the guardian email appeared in Mailpit; confirmation was not completed. |
| Report answer | Not implemented in current API/client | Report table/entity exists; report controller and Android action are absent. |
| Account deletion | Implemented, not verified live in P19 | Backend deletion job and Android privacy call exist; no P19 account-deletion run. |
| Redis outage / wrong-key / rate-limit failure demos | Not executed | Procedures are documented in [DEMO_SCRIPT.md](../../DEMO_SCRIPT.md); no failure-demo evidence. |
| Usability sessions | Not executed | [USABILITY_TEST.md](../../USABILITY_TEST.md) retains “Not executed (awaiting peer/student sessions).” |

## Release decision

- Acceptance is **blocked**, not green. Backend readiness and APK installation are verified, but requested user journeys remain unexecuted and multiple features are absent or incomplete in the current client/API implementation.
- A partial local walkthrough captured the linked answer, bookmark, and pre-fix History screenshots above; no full P19 screen recording or acceptance run was completed. Existing P08/P18 evidence is not substituted for P19 execution.
- **No release-candidate tag was created.** Do not tag until the backend startup cause is resolved, required user journeys are verified, and the checklist is green.

See the [P19 evidence index](./README.md) for captured build/test outputs and the sanitized startup diagnosis/recovery note.
