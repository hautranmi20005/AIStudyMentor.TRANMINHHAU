# P17 Test Plan

## Purpose and strategy

This plan supports BTEC M1 by defining the test strategy, environments, test levels, and entry/exit criteria for the AI Study Mentor Android client and Spring Boot API. It uses a risk-based, layered strategy: fast isolated unit tests first, real MySQL/Redis integration tests next, then Android emulator journeys. API and Android tests complement one another; neither substitutes for the other.

### Test environment

| Environment | Requirements | Use |
|---|---|---|
| Backend local/CI | Temurin JDK 17.0.21, Maven wrapper, Docker daemon, MySQL 8.0.36, Redis container | Maven unit, integration, and API tests |
| Android local/CI | Temurin JDK 17.0.21, Android SDK 34, Gradle wrapper | Android unit tests and lint |
| Android emulator | Connected device named `emulator-5554`, Android API level supported by the app | Room instrumentation and core UI journeys |
| AI provider | Stubbed provider or fake Gemini adapter in automated tests | Deterministic output; no real Gemini credentials or provider calls |

Testcontainers must not silently skip when Docker is unavailable. Integration-test setup must fail visibly so that a green test report cannot conceal missing infrastructure. The CI runner has Docker available. Emulator UI tests remain local-only: standard GitHub-hosted Android runners do not guarantee nested KVM/hardware acceleration needed for a reliable emulator, so CI runs Android unit tests and lint while the connected-emulator suite is an explicit local gate.

### Test levels

| Level | Scope and technique | Success evidence |
|---|---|---|
| Unit | JUnit/Mockito for backend services and Kotlin/JUnit for ViewModels, repositories, validators, formulas, and cache/quota behavior | Surefire or Gradle XML result with no unreviewed failures |
| Integration | Spring Boot with disposable MySQL and Redis Testcontainers; Android Room DAO and migration instrumentation | Tests demonstrate persistence, isolation, migration, and real Redis/MySQL behavior |
| Contract | Jackson/DTO serialization and standardized API error response assertions; Retrofit converter checks | Required fields, types, status/code, and safe serialization match the contract |
| API end-to-end | Authenticated API workflows using stubbed/fake AI and isolated persistence | Each workflow asserts response, persisted read-back, and cleanup where applicable |
| Emulator UI | Connected Android journeys with Compose UI tests and in-memory Room | Visible user outcome and local persistence are verified on `emulator-5554` |
| Manual usability | Peer/student sessions using [USABILITY_TEST.md](./USABILITY_TEST.md) | Observation sheets and consent are completed; no participant result is presumed |

### Entry and exit criteria

**Entry**

- The requested branch is checked out and existing worktree changes are preserved.
- Temurin JDK 17.0.21 is selected for Maven and Gradle.
- Docker is running before backend integration tests begin.
- Backend test sources and Android `testDebugUnitTest`/`androidTest` sources compile.
- Test accounts and test doubles are isolated; no production provider keys or participant data are used.

**Exit**

- Maven and Android unit/lint reports are captured with actual pass/fail/skip counts.
- Test failures and skips are reported as such; unavailable capabilities are identified explicitly.
- MySQL/Redis integration tests run without Docker-based silent skips.
- Connected tests are run only when the required emulator is attached; otherwise they are recorded as not run, not passed.
- All manually executed cases have evidence, and usability execution results remain “Not executed (awaiting peer/student sessions)” until real sessions take place.

## P17 execution results

Local verification used Temurin JDK 17.0.21 and a running Docker daemon. Maven `verify` completed with 88 tests, 0 failures, 0 errors, and 0 skips; Android `testDebugUnitTest` completed with 17 tests, 0 failures, and 0 skips; `lintDebug` passed; and `connectedDebugAndroidTest` completed with 17 tests, 0 failures, and 0 skips. See [TEST_REPORT.md](./TEST_REPORT.md) for commands, evidence, benchmark results, and remaining gaps.

## Test cases

| ID | Requirement | Scenario | Input | Expected | Actual | Status | Evidence |
|---|---|---|---|---|---|---|---|
| TC-01 | Account registration and onboarding | Register an adult account, complete MFA, and save the student profile | Unique email, valid password, adult date of birth, TOTP code, education level | Registration succeeds; MFA gates the protected session; profile is persisted and retrievable | Full API flow completed; TOTP verification and onboarding profile read-back passed | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-02 | Guardian consent | Register an under-13 student and attempt a protected study action before guardian approval | DOB under 13, distinct guardian email, valid credentials | Consent is pending; protected study APIs reject access until approval | API test confirmed pending consent, guardian email, and 403 on protected questions | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-03 | Question answering and history | Submit a text question and retrieve its saved answer | Authenticated text question | Structured answer is returned and can be read back from history | Stubbed-AI API flow returned the structured answer and retrieved the persisted question | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-04 | MFA security | Submit invalid MFA codes to the configured threshold | MFA session and repeated invalid codes | Invalid code is rejected; rate limit blocks further verification and returns the standard error contract | Valid TOTP challenge/verification is covered end-to-end; invalid-code threshold/lockout was not separately exercised | Partial | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-05 | Photo question | Submit an allowed image question through the fake/stub AI adapter | Valid image bytes, MIME type, and question text | Image is validated, accepted, and the answer is persisted without exposing image payloads in logs | Image validation, upload service, and Gemini image adapter have unit coverage; no photo-question HTTP flow is wired/tested end-to-end | Partial | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-06 | AI response cache | Repeat the same eligible question for the same profile | Identical normalized question and context | The second request is served as a cache hit; a different profile/context does not reuse it | Orchestrator cache-hit branch and profile-scoped key generation passed unit tests; no Redis cache-hit round-trip assertion | Partial | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-07 | Quiz integrity | Submit a quiz response containing a tampered or foreign option/question identifier | Valid attempt plus altered response identifier | The response cannot earn credit for another question/quiz; scoring is based on the user's actual attempt | Quiz tampering/service checks passed; API quiz generation, submission, grading, and history read-back passed | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |
| TC-08 | Offline access | Open previously cached questions/bookmarks with network unavailable | Cached account data and simulated offline state | Cached content remains available; writes are queued or clearly reported for later sync | Account-scoped Room cache/migration and queued bookmark retry tests passed; a full airplane-mode UI journey was not run | Partial | [TEST_REPORT.md](./TEST_REPORT.md), [android-connected-retest.log](./evidence/P17/android-connected-retest.log) |
| TC-09 | Account deletion | Re-authenticate and delete the account, then query protected resources | Valid password/MFA and authenticated session | Account and associated personal data are removed or de-identified per policy; subsequent access is denied | Full API lifecycle confirmed deletion returns 204; Android confirmation/re-authentication screen test also passed | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log), [android-connected-retest.log](./evidence/P17/android-connected-retest.log) |
| TC-10 | API error contract | Trigger validation and business errors | Invalid DTO and representative domain error | HTTP status and JSON fields follow the shared error DTO; rejected secrets and exception text are not returned | Global exception handler contract tests passed | Pass | [TEST_REPORT.md](./TEST_REPORT.md), [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) |

## Data handling and evidence

- Generate unique account identifiers per test and use disposable databases/containers.
- Tests use the stub provider or fake provider adapter, never a real AI key.
- Use synthetic test identities only. Do not copy real participant data into reports.
- XML/HTML build reports and benchmark summaries are stored under [evidence/P17/](./evidence/P17/).

## P19 final-acceptance recheck

The P17 execution table above is a historical P17 result. In the P19 recheck, Temurin JDK 17.0.21, healthy local MySQL/Redis/Mailpit services, and `emulator-5554` were available. Android reports show 18 unit tests and 18 connected tests passed, with no failures, errors, or skips; `lintDebug` reported zero `HardcodedText` findings. After correcting the local datasource/Redis environment overrides, `.\scripts\demo-up.ps1` completed and authenticated Actuator health returned `UP`; the APK was installed and launched. No authenticated user journey was performed. The separate Maven `verify` run was interrupted during `BackendEndToEndIntegrationTest`: 29 tests in nine fresh XML reports completed with 0 failures/errors/skips, but the suite has no final Maven status and is not a pass. See the [partial Maven evidence](./evidence/final/backend/MAVEN-VERIFY-STATUS.md) and [startup diagnosis](./evidence/final/backend-startup.md).

The emulator was able to install and launch the debug APK to the sign-in screen; this was not an authenticated app journey. No P19 end-to-end walkthrough or failure demo is claimed. The [acceptance checklist](./evidence/final/ACCEPTANCE_CHECKLIST.md) is not green and no RC tag was created.
