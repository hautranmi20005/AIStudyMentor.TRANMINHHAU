# P12 Dashboard Validation

## Automated results

- Backend: `backend\mvnw.cmd test` — **91 tests passed**, 0 failures/errors. Includes the seeded MySQL/Testcontainers dashboard endpoint case and Flyway schema validation.
- Android unit tests: `:app:testDebugUnitTest` — **17 tests passed**, 0 failures.
- Android instrumentation: `:app:connectedDebugAndroidTest` — **20 tests passed**, 0 failures on the connected API 37 emulator. Includes dashboard empty/loaded/error UI states, Room DAO account isolation, and the Room 3-to-4 migration test.
- Android instrumentation sources compiled successfully with `:app:compileDebugAndroidTestKotlin`.
- `scripts\seed-dashboard.ps1` and `scripts\verify-dashboard.ps1` passed PowerShell parser validation.
- `git diff --check` passed for the P12 changes.

## Dashboard integration case

The backend integration test seeds questions and quiz attempts for one profile, requests both `30d` and `7d` metrics in `Asia/Ho_Chi_Minh`, checks weighted accuracy and weak-topic numerator/denominator, checks the empty-account response, and verifies invalid timezones return `400`. The original failure was caused by the test client double-encoding `%2F`; using a raw `URI` fixed the request.

## Not run

- The SQL/API PowerShell verification and seed scripts were not executed because this session had no supplied database credentials or authenticated API token. Provide `DB_PASSWORD`, `DASHBOARD_ACCESS_TOKEN`, an existing profile UUID, and the backend storage timezone to run them.
- No manual signed-in dashboard journey or production-like seed data was executed on the emulator. Instrumented tests verify UI states and the Room migration, not live API-to-Room synchronization or account-login behavior.
