# P16 Performance Report

**Status:** JDK 17 build recovery and local stack startup verified. A six-worker live-app smoke test was captured, but the requested 50-user/120-second baseline is blocked by failed Gemini-backed ask requests and the HTTP route not using the configured stub. No optimizations were applied.
**Branch:** `feature/p16-performance` at `2579e16`
**Last checked:** 2026-11-17

The referenced `PERFORMANCE_REPORT_2.md` was not found in the workspace; this file is the current P16 report.

## Result summary

No performance optimization was applied: a valid live backend or current-device Android baseline could not be collected, and the task requires measurements before changes. NFR-01 and NFR-05 are **not claimed**.

| Area | Before | After | Status |
|---|---|---|---|
| Backend HTTP scenarios (50 users, 2 minutes) | Not measured | Not run | Six-worker diagnostic smoke only; see below |
| Backend source compilation (JDK 17) | Failed with source errors | Pass in isolated copy | Main source compiles; full test compilation has unrelated test-source errors |
| Android app Kotlin/KSP compilation (JDK 17) | Failed with source errors | Pass in isolated copy | `:app:compileDebugKotlin` succeeds; no APK/device profiling run |
| Android cold startup (existing APK, n=5) | p50 6,517 ms; p95/p99 9,112 ms | N/A | Exploratory only; APK predates current source |
| Android warm startup (existing APK, n=5) | `TotalTime` 0 ms; `WaitTime` p50 17 ms, range 10–19 ms | N/A | Exploratory only |
| Android process memory (existing APK, n=2) | PSS 107,447–108,263 kB | N/A | Exploratory only |
| History/Answer long-content jank and memory | Not measured | N/A | No current-source build or frame trace |
| 1–2 GB AVD / slow network | Not measured | N/A | Low-RAM AVD did not start |
| Real Gemini text/image latency and tokens | Not measured | N/A | Two text-ask attempts failed; no successful provider response or token data; no image sample |

Raw exploratory startup rows are in [android-startup-baseline.csv](./evidence/P16/android-startup-baseline.csv). They are not a valid before/after comparison.

## Environment and method

- Windows; repository Maven and Gradle wrappers are present. Temurin JDK 17.0.21+9 is installed and selected for the Maven and Gradle verification commands; Maven 3.9.6 reports Java 17.0.21.
- The backend main source compiles in an isolated copy on JDK 17, and five focused `GeminiAdapterServiceTest` tests pass. Full Maven test compilation is still blocked by unrelated `GlobalExceptionHandlerTest` API mismatches. The Android `:app:compileDebugKotlin` task, including KSP, succeeds in an isolated copy on JDK 17. Instrumentation-test source compilation still has stale test/API mismatches; no instrumentation tests were run. Copies were used to avoid replacing tracked build outputs.
- The authorized local dev-volume reset was completed; MySQL, Redis, Mailpit, and backend actuator health all reported `[OK]` after startup and again after the smoke test. Local DB/Redis credentials were aligned and the requested JWT, MFA, and management secrets were populated without displaying values. The configured Gemini key was not printed or independently validated.
- A private 50-user/quiz fixture was created in the local development database. The temporary JSON file containing test passwords/tokens was deleted after the smoke run; the local fixture rows remain available for follow-up testing.
- The dev profile resolves DB, Redis, JWT/MFA, mail, and Gemini settings from the environment imported by `dev-backend.ps1`. Flyway automatic cleaning remains disabled. Compose syntax validation and authenticated service health checks pass.
- No backup `.env` file was found. The reference template contains development defaults, not a backup, and has no `DB_ROOT_PASSWORD`; no template secrets were copied.
- The prior exploratory startup method used five `am start -W` cold launches after `am force-stop`, five warm launches, and two `dumpsys meminfo` snapshots. The APK existed before the current-source build attempts. The low-RAM AVD attempt was rejected because the existing AVD was already running and reported 4 GB.
- Current diagnostic outcomes and build details are in [build-blockers.txt](./evidence/P16/build-blockers.txt) and [android-baseline-environment.txt](./evidence/P16/android-baseline-environment.txt).

## Backend load-test harness

[P16-LoadTest.ps1](../scripts/loadtest/P16-LoadTest.ps1) runs a mixed workload with 50 concurrent workers for 120 seconds by default. It writes timestamped raw request samples and per-scenario p50/p95/p99, error rate, throughput, and observed cache-hit signals under `docs/evidence/P16/`. The example input shape is [p16-input.example.json](../scripts/loadtest/p16-input.example.json); real credentials and IDs must be supplied in a private local file, not committed. `-ValidateOnly` checks the JSON shape without sending requests.

The six scenarios are login challenge, unique text ask, warmed repeat ask, paged history, dashboard metrics, and quiz grading. Quiz grading starts a fresh attempt before measuring the submit call. The login measurement ends at the login response; it does not include MFA verification. The repeated-ask row records an explicit cache-hit signal only if the response exposes `cacheHit` or `servedFromCache`.

The harness passed input validation and was run against the live dev backend with six concurrent workers (one worker per scenario), 50 fixture records, and a one-second target duration. The run took 60.05 seconds because the unique ask timed out at the harness's 60-second client deadline. It produced 163 measured requests: 161 successful non-AI requests and two failed AI asks (one HTTP 502 and one client timeout), for a mixed-run error rate of 1.23%. This is diagnostic smoke evidence, not the requested 50-user/120-second baseline; per-scenario request counts are too small for representative percentiles. The smoke CSV and summary are [backend-load-smoke-20261117.csv](./evidence/P16/backend-load-smoke-20261117.csv) and [backend-load-smoke-summary-20261117.json](./evidence/P16/backend-load-smoke-summary-20261117.json).

Source inspection found that `QuestionService` calls `GeminiAdapterService` directly; `StubAiProvider`, cache, semaphore bulkhead, and single-flight behavior are in `AiOrchestrator`, which is not on this HTTP path. The harness's `provider` and `stubLatencyMs` fields are report metadata only and do not configure server behavior. Therefore this smoke used the Gemini-backed path; it is not a stub-isolated measurement. No real-provider response/token measurement was completed.

### Live backend smoke (diagnostic only; not a baseline)

The harness target was one second, but its actual wall-clock duration was 60.05 seconds. Throughput below is the harness's per-scenario request count divided by that total wall time. Single-sample ask percentiles are recorded for traceability only and are not statistically meaningful.

| Scenario | Requests | Errors / rate | p50 ms | p95 ms | p99 ms | Throughput req/s | Observation |
|---|---:|---:|---:|---:|---:|---:|---|
| Login | 9 | 0 / 0% | 105.06 | 186.73 | 186.73 | 0.150 | All responses succeeded |
| Ask text | 1 | 1 / 100% | 60,018.03 | 60,018.03 | 60,018.03 | 0.017 | Client timeout; no HTTP status |
| Cache-hit ask | 1 | 1 / 100% | 2,536.80 | 2,536.80 | 2,536.80 | 0.017 | HTTP 502; no cache-hit signal |
| History list | 43 | 0 / 0% | 19.63 | 31.81 | 60.80 | 0.716 | All responses succeeded |
| Dashboard | 96 | 0 / 0% | 7.10 | 11.25 | 106.29 | 1.599 | All responses succeeded |
| Quiz grading | 13 | 0 / 0% | 37.32 | 99.45 | 99.45 | 0.216 | All responses succeeded |

### Backend measurements and findings

| Scenario/control | Before | After |
|---|---|---|
| Six scenarios, requested 50-user / 2-minute workload | Not measured | Not run; six-worker diagnostic smoke above is not a baseline |
| 50 users / 2 minutes: p50, p95, p99, errors, throughput | Not run | N/A |
| History, bookmark, dashboard `EXPLAIN` plans | Not run; database now available | N/A |
| N+1, projection DTOs, page-size bounds, long-TEXT list reads | Not changed without measurements | N/A |
| Hikari sizing, Jackson, response compression | Not changed without measurements | N/A |
| Gemini bulkhead, deduplication, timeout and output limits on HTTP path | Not measured; orchestrator not wired to question submission | N/A |

Source inspection indicates:

- The question list maps a page of entities and performs `existsByProfileAndQuestion` per row. Bookmark listing also maps full question entities. The list paths may load `question_text` and have an N+1 risk; query plans and timings are required before changing them.
- `DashboardService` loads all questions, quizzes, and attempts before grouping in memory. Migration V11 already adds dashboard-related indexes, but no query plan or matched latency comparison was collected. The separate dashboard aggregation code path also needs build/runtime verification.
- Question and quiz controllers create `PageRequest` directly. No bound is applied in these controllers. The current checked-in code, not API prose, must be used for future validation.
- `AiQuotaService` has per-profile daily defaults of 100 calls and 100,000 tokens, reserving 8,192 tokens per request. A global daily token budget and `AI_BUDGET_EXCEEDED` response were not found. The management `/actuator/ai-cost` report and `scripts/ai-cost-report.ps1` provide process-local token/cost estimates, not billing measurements.
- No verified HTTP token counts or prompt-token before/after data exist for this path. Do not infer Gemini cost per question or quiz from the stub, cache estimates, or configured price assumptions.

## Android measurements and findings

| Scenario/control | Before | After |
|---|---|---|
| Cold/warm startup and PSS | Exploratory values above, existing APK only | N/A |
| History and Answer long-content jank/recomposition | Not measured | N/A |
| Low-RAM and slow-network behavior | Not measured | N/A |
| Coil downsampling, cache/timeouts, gzip, unchanged-data fetch, cancellation | Not measured | N/A |

The History screen already supplies stable LazyColumn keys and collects state with lifecycle awareness. The Answer detail list has no stable keys. Neither was profiled, so no recomposition or rendering optimization was made. `NetworkModule` builds a default OkHttp client with no explicit cache or timeout tuning. No frame-timeline/Macrobenchmark trace, long-content fixture, or low-RAM device result is available.

## Real Gemini latency and cost

Two text-ask requests reached the Gemini-backed HTTP path during the smoke test; one returned HTTP 502 in 2.54 seconds and one exceeded the 60-second client timeout. No successful provider response or token-usage data was captured, and no image request was made. Text/image p50/p95 provider latency, measured cost per question/quiz, and prompt-token counts are therefore **unavailable**, not zero. The Gemini key was not independently validated. The existing cost endpoint's estimated cost is not a substitute. Any future 20-request text/image sample must state network and model variability. Streaming was not introduced.

## Remaining work

### Latest environment and measurement recheck (2026-11-17)

- MySQL, Redis, Mailpit, and backend actuator health all pass. The local-volume reset and startup were explicitly authorized. The load-test smoke used local fixtures; its temporary credential/token file was removed, and no secrets were logged.
- The requested 50-user/120-second workload was not run. The two AI smoke calls failed (HTTP 502 and a 60-second client timeout), and the HTTP route does not use the requested configurable stub. Do not treat the 6-worker smoke table as the P16 baseline or claim NFR-01/NFR-05.
- No optimization or query-plan change was made. Capture the stub-isolated 50-user baseline (or repair/validate the Gemini-backed path and agree to its provider-inclusive cost) before choosing backend optimizations, then repeat the same workload for an after comparison.
- Android current-source startup, memory, long-content jank, and low-RAM/slow-network measurements remain outstanding. A separate successful 20-request real-Gemini text/image sample with token/cost data is also outstanding.

No NFR-01/NFR-05 or production-performance claim is made.
