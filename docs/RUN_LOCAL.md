# Run AI Study Mentor Locally

This setup is for local development and Android Studio emulators only. The dev profile uses MySQL, Redis, and Mailpit from the root `docker-compose.dev.yml`; it does not change the PostgreSQL production artifacts.

## Prerequisites

- Docker Desktop with Docker Compose v2 (`docker compose up --wait` support).
- JDK 17 on `PATH` or `JAVA_HOME`; the backend Maven wrapper is used.
- Android Studio, Android SDK Platform 34, and an Android emulator.
- A real Gemini API key configured in the ignored `backend/.env` to use Ask AI. The backend can start without the key and logs a warning; AI requests then return `AI_NOT_CONFIGURED`. `scripts/demo-up.ps1` requires a real key. Do not use a stub as a demo provider.
- PowerShell on Windows, or Bash and Python 3 on Unix-like systems.

## Six steps: start, then register and sign in on the emulator

Run commands from the repository root unless noted.

1. Generate the ignored local environment file only for a new local database/Redis setup with no existing Docker volumes:

   ```powershell
   .\scripts\gen-secrets.ps1
   ```
   ```bash
   bash ./scripts/gen-secrets.sh
   ```

   The helper replaces the entire file when explicitly confirmed, including its DB/Redis/JWT/MFA values and any Gemini key. Do not use it to repair an existing installation: new DB passwords will not match credentials stored in an existing MySQL volume, and changing the MFA key can make stored MFA data unreadable. Restore the original ignored file from a secure backup/password manager instead. Never paste secrets into source, terminal logs, or chat.

2. Edit `backend/.env` locally and set `GEMINI_API_KEY` to a real provider key. Never paste the value into source, terminal logs, or chat.

3. Start MySQL, Redis, and Mailpit. The service ports bind only to `127.0.0.1`.

   ```powershell
   .\scripts\dev-up.ps1
   ```
   ```bash
   bash ./scripts/dev-up.sh
   ```

4. Start Spring Boot in a terminal and leave it running. The dev launcher skips compiling test sources; run tests separately. Flyway applies migrations on startup; Hibernate validates the schema.

   ```powershell
   .\scripts\dev-backend.ps1
   ```
   ```bash
   bash ./scripts/dev-backend.sh
   ```

`AI_PROVIDER` defaults to `gemini`. Dev and production profiles require valid Gemini configuration and reject the test/load-test-only `stub` provider. Gemini uses a 5-second connect timeout and 30-second read timeout by default; override with `GEMINI_CONNECT_TIMEOUT_MS` and `GEMINI_READ_TIMEOUT_MS`.

5. In a second terminal, check all three dependencies and the authenticated management health endpoint. Expected output is four `[OK]` lines.

   ```powershell
   .\scripts\dev-status.ps1
   ```
   ```bash
   bash ./scripts/dev-status.sh
   ```

6. Build/install and start the debug app, then sign in with the middle-school demo account. In Android Studio, select the emulator and run the `app` debug configuration; alternatively:

   ```powershell
   cd android
   .\gradlew.bat :app:installDebug
   adb shell monkey -p com.aistudymentor 1
   ```

   Sign in using `demo.middle@local.test` and `DemoPass123!`. Generate the current MFA code in another terminal with `.\scripts\totp.ps1` or `bash ./scripts/totp.sh`, then enter it in the app.

The backend is available at `http://127.0.0.1:8080`; the emulator reaches the host through `http://10.0.2.2:8080/api/v1/`. For a physical Android device, connect it with USB, run `adb reverse tcp:8080 tcp:8080`, set `DEBUG_API_BASE_URL=http://127.0.0.1:8080/api/v1/` in `android/local.properties`, and rebuild/reinstall the debug app.

## Auth smoke test

This script registers a unique test account, signs it in, and then checks a seeded demo account's TOTP flow. It prints only statuses, never tokens.

```powershell
.\scripts\smoke-auth.ps1
```
```bash
bash ./scripts/smoke-auth.sh
```

## AI smoke test

After obtaining an authenticated access token through the normal sign-in/MFA flow, run a real structured-answer smoke test. The script refuses stub responses and never prints the token or answer content:

```powershell
$env:AI_SMOKE_BEARER_TOKEN = "<short-lived access token>"
.\scripts\smoke-ai.ps1
```

For a bounded quality run, see [docs/ai-benchmark/README.md](./ai-benchmark/README.md). Real benchmark runs call Gemini and consume provider tokens; the checked-in report says whether real measurements have actually been collected.

Mailpit captures local email at [http://127.0.0.1:8025](http://127.0.0.1:8025). The under-13 demo's pending guardian-consent email is sent there.

## Development-only demo accounts

`@Profile("dev")` gates all seed creation. Other Spring profiles do not create these accounts. Seeding runs once per account and does not reset existing users.

| Email | Password | Education level | Status |
|---|---|---|---|
| `demo.middle@local.test` | `DemoPass123!` | Middle School | Active |
| `demo.high@local.test` | `DemoPass123!` | High School | Active |
| `demo.university@local.test` | `DemoPass123!` | University | Active |
| `demo.pending@local.test` | `DemoPass123!` | High School, age 12 | Pending guardian consent |

All seeded users have a verified TOTP factor using the development-only Base32 secret `JBSWY3DPEHPK3PXP`. It is public test data, not a production credential. The pending account cannot access user-owned resources until guardian consent is approved.

## Status, shutdown, and reset

- `scripts/dev-status.ps1` / `scripts/dev-status.sh` checks MySQL, Redis, Mailpit, and backend health.
- `scripts/dev-down.ps1` / `scripts/dev-down.sh` stops containers but preserves named volumes.
- `scripts/dev-reset.ps1` / `scripts/dev-reset.sh` asks for confirmation, deletes local service volumes, starts clean dependencies, and starts the backend so Flyway recreates the schema. Pass `-Force` (PowerShell) or `--force` (Bash) to skip the prompt. Reset does not delete source files.

## Troubleshooting

| Symptom | Check / resolution |
|---|---|
| A local port is already in use | Check ports 3306, 6379, 8025, 8080, and 8081. Stop the conflicting local service or change the corresponding dev port before starting Compose/backend. |
| Emulator cannot reach the backend | Confirm backend is listening on 8080, use `10.0.2.2` (not `localhost`) from the emulator, and use the debug build. For a USB device, run `adb reverse tcp:8080 tcp:8080`. |
| Flyway checksum mismatch | Do not edit an applied V1–V4 migration. Restore the original migration or create a forward-only V5+ migration after reviewing the schema state. |
| Docker is not running | Start Docker Desktop, wait until its engine is ready, then rerun `dev-up`. |
| Backend reports missing environment variables | Check `backend/.env` has generated JWT/MFA/DB/Redis/management values and real Gemini key, model, and API version. The startup error names missing keys without printing their values. |
| Management health is unauthorized or unavailable | Keep the backend running, use the generated management credentials through `dev-status`, and verify local port 8081 is available. The management listener binds to loopback only. |
| Schema validation fails after reset | Check Docker dependency health and startup output. `dev-reset` removes volumes before recreating the schema; do not alter applied migrations to silence validation. |

Compose host bindings can be changed without editing the Compose file by setting `DEV_MYSQL_PORT`, `DEV_REDIS_PORT`, `DEV_MAIL_SMTP_PORT`, and/or `DEV_MAIL_UI_PORT` in the shell before `dev-up`. For example:

```powershell
$env:DEV_MYSQL_PORT = "13306"
$env:DEV_REDIS_PORT = "16379"
$env:DEV_MAIL_SMTP_PORT = "11025"
$env:DEV_MAIL_UI_PORT = "18025"
.\scripts\dev-up.ps1
```
```bash
export DEV_MYSQL_PORT=13306 DEV_REDIS_PORT=16379 DEV_MAIL_SMTP_PORT=11025 DEV_MAIL_UI_PORT=18025
bash ./scripts/dev-up.sh
```

If changing MySQL, Redis, or SMTP host ports, also update `DB_URL`, `REDIS_PORT`, or `MAIL_PORT` in `backend/.env` for the backend's host connection.

## Automated test suites

Use Temurin JDK 17.0.21 for both Maven and Gradle. Backend integration tests use Testcontainers and require a running Docker daemon; they fail instead of silently skipping if Docker is unavailable. CI runs backend tests on a Docker-enabled runner.

From the repository root, run backend tests:

```powershell
$env:JAVA_HOME = "C:\Path\To\Temurin\jdk-17.0.21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Push-Location backend
.\mvnw.cmd --batch-mode --no-transfer-progress verify
Pop-Location
```

Run Android unit tests and lint:

```powershell
$env:JAVA_HOME = "C:\Path\To\Temurin\jdk-17.0.21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Push-Location android
.\gradlew.bat --no-daemon "-Pkotlin.compiler.execution.strategy=in-process" :app:testDebugUnitTest :app:lintDebug
Pop-Location
```

Room migration/DAO and Compose UI tests require the local `emulator-5554` device. Check `adb devices -l` before running them:

```powershell
Push-Location android
.\gradlew.bat --no-daemon "-Pkotlin.compiler.execution.strategy=in-process" :app:connectedDebugAndroidTest
Pop-Location
```

The emulator suite is intentionally local-only. Standard hosted CI runners do not guarantee nested KVM or Android hardware acceleration; CI therefore runs Android unit tests and lint, and uploads their XML/HTML reports. Test reports and P17 benchmark evidence are collected under `docs/evidence/P17/`.
