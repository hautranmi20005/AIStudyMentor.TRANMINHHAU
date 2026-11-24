# P17 Test Report

This is the P17 execution record for the worktree state in which those tests ran. It is not a P19 acceptance result. The current P19 source/API audit found implementation wiring gaps; use [the P19 checklist](./evidence/final/ACCEPTANCE_CHECKLIST.md) for the current status.

## Summary

Local test execution was performed with Temurin JDK 17.0.21. Docker was available for Testcontainers. Tests use disposable MySQL 8.0.36 and Redis 7.2 containers, with Gemini and email integrations stubbed in application tests.

| Suite | Command/result | Passed | Failed | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| Backend Maven unit + integration | `backend\mvnw.cmd --batch-mode --no-transfer-progress verify` — exit 0 | 88 | 0 | 0 | 0 |
| Android JVM unit tests | `:app:testDebugUnitTest` — exit 0 | 17 | 0 | 0 | 0 |
| Android lint | `:app:lintDebug` — exit 0 | Pass | 0 | 0 | 0 |
| Android connected instrumentation | `:app:connectedDebugAndroidTest` — exit 0, connected emulator | 17 | 0 | 0 | 0 |
| Android test-source compilation | unit and `androidTest` Kotlin compilation — exit 0 | Pass | 0 | 0 | 0 |

Counts are aggregated from Maven Surefire XML (33 report files), Android unit XML (8 files), and connected instrumentation XML (1 file). No final test was skipped. The emulator suite ran locally; it is deliberately not run on standard hosted CI runners because nested KVM/hardware acceleration is not guaranteed.

The final backend run includes five API integration scenarios using MySQL and Redis Testcontainers and one Flyway/MySQL schema validation test. The complete lifecycle scenario covers MFA verification, onboarding, a stubbed text question and persistence, bookmarking, quiz submission, dashboard metrics, and account deletion. Under-13 registration and guardian-consent blocking are separately exercised.

The backend test-source audit found no `@Disabled` or `disabledWithoutDocker = true` integration tests. Testcontainers startup is required; Docker unavailability fails the test run rather than silently skipping or falling back to an in-memory database.

## Coverage and limitations

- **Backend:** Unit coverage includes consent, AI safety/cache/quota, image validation/upload, quiz behavior, dashboard formulas, exception contracts, and security rules. The end-to-end suite verifies authenticated persistence and account deletion against containerized MySQL/Redis.
- **Photo questions:** Image content validation, upload services, and the Gemini image adapter have unit coverage. The current API does not expose a complete photo-question HTTP path, so upload → question → persisted answer is not verified end-to-end. There is also no Android photo-question UI journey test.
- **MFA:** The API lifecycle verifies a valid TOTP challenge and response. Invalid-code threshold/lockout behavior is not separately exercised.
- **Cache:** Cache-key normalization and profile scoping, plus the orchestrator's cached-response branch, are unit-tested. A real Redis cache-hit round trip is not asserted.
- **Offline mode:** Room account-scoped history/bookmark persistence, migrations, and queued bookmark retry behavior are exercised on the emulator. A full airplane-mode UI journey is not covered.
- **Android UI journeys:** Tests cover login routing, text-question submission and explanation rendering, dashboard states, quiz entry-point rendering, and account-deletion confirmation. These are separate instrumented checks, not a single UI-driven Auth/Onboarding → Ask Text/Image → History/Bookmark → Quiz → Dashboard → Delete journey. Quiz UI submission and account-deletion completion are not asserted through the full UI.
- **JaCoCo:** No JaCoCo report was generated; JaCoCo is not configured in the Maven project.
- **CI:** The GitHub Actions workflow was updated to run backend verification, Android unit tests, lint, and report artifact uploads. The workflow itself was not triggered as part of this local run.
- **Usability:** No participants were recruited; results remain **“Not executed (awaiting peer/student sessions)”** in [USABILITY_TEST.md](./USABILITY_TEST.md).

## AI benchmark and team baseline targets

Python 3.15.2 was available, and the P06 runner executed all 42 records against the deterministic local stub evaluator: 32 answer samples and 10 adversarial/refusal samples.

| Metric | Team Baseline Target | Observed |
|---|---:|---:|
| JSON Schema Validity | 100% | 32/32 (100%) passed the runner's required-field checks; not an independent JSON Schema conformance suite |
| Scope Refusal Accuracy | >= 90% | 10/10 (100%) |
| Zero Prompt/Secret Leakage | 100% | Not measured by this harness |

The 10 adversarial cases correctly returned HTTP 422 `AI_OUT_OF_SCOPE`. The benchmark labels these expected refusals as failed/errored HTTP requests; they are not failed refusal checks. The stub run does not measure real Gemini quality, prompt-injection resistance, malformed model output, or leakage. See the [benchmark report](./evidence/P17/p06-stub-benchmark-report.md) and [console output](./evidence/P17/p06-stub-benchmark-console.log).

## Stability and coverage notes

No flaky tests were observed in the final successful runs. An earlier full Maven attempt exposed a deterministic NPE in `AiOrchestratorCacheTest`: the test stubbed the obsolete cache-key method instead of `keyForUser`. The test stub was corrected, and the final full Maven `verify` rerun passed. Earlier failed/baseline logs are retained and labeled as historical evidence below; they are not the final result.

## Evidence index

All paths below are relative to this report. Historical logs document troubleshooting or pre-fix attempts and must not be interpreted as the final suite result.

### Final execution evidence

- [backend-mvn-verify-final.log](./evidence/P17/backend-mvn-verify-final.log) — final Maven `verify`, exit 0.
- [android-unit-lint.log](./evidence/P17/android-unit-lint.log) — Android unit tests and `lintDebug`, successful.
- [android-connected-retest.log](./evidence/P17/android-connected-retest.log) — connected emulator tests, successful.
- [android-test-compile.log](./evidence/P17/android-test-compile.log) — Android test-source compilation, successful.
- [backend-full-flow-retest.log](./evidence/P17/backend-full-flow-retest.log) — targeted full API lifecycle run.
- [p06-stub-evaluator.py](./evidence/P17/p06-stub-evaluator.py) — deterministic local evaluator harness.
- [p06-stub-benchmark-report.md](./evidence/P17/p06-stub-benchmark-report.md) — benchmark measurements, targets, and limitations.
- [p06-stub-benchmark-console.log](./evidence/P17/p06-stub-benchmark-console.log) — raw benchmark console output.

### Historical baseline/troubleshooting evidence

- [backend-mvn-verify.log](./evidence/P17/backend-mvn-verify.log) — initial full-suite run before the cache mock correction; one deterministic test error.
- [backend-mvn-test.log](./evidence/P17/backend-mvn-test.log) — earlier backend test attempt; superseded by final `verify`.
- [backend-integration-retest.log](./evidence/P17/backend-integration-retest.log) — integration troubleshooting history; superseded by final `verify`.
- [backend-test-compile-baseline.log](./evidence/P17/backend-test-compile-baseline.log) — initial backend test-compilation baseline.
- [android-connected-tests.log](./evidence/P17/android-connected-tests.log) — earlier connected-test attempt; superseded by connected retest.
- [android-test-compile-baseline.log](./evidence/P17/android-test-compile-baseline.log) — initial Android test-compilation baseline.

