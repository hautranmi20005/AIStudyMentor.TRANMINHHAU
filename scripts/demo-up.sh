#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/dev-common.sh"

if [[ ! -f "$DEV_ENV_FILE" ]]; then
    printf '%s\n' "backend/.env is missing. Create it from backend/.env.example and set GEMINI_API_KEY." >&2
    exit 1
fi

load_dev_environment

if [[ -z "${GEMINI_API_KEY:-}" || "$GEMINI_API_KEY" == "<SET_ME>" ]]; then
    printf '%s\n' "GEMINI_API_KEY is missing or unset in backend/.env. Its value was not displayed." >&2
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
    printf '%s\n' "Set JAVA_HOME to Temurin JDK 17.0.21 before running the demo." >&2
    exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
if ! java -version 2>&1 | grep -q '17\.0\.21'; then
    printf '%s\n' "Demo requires Temurin JDK 17.0.21; JAVA_HOME points to a different version." >&2
    exit 1
fi

if ! command -v docker >/dev/null 2>&1 || ! docker info --format '{{.ServerVersion}}' >/dev/null 2>&1; then
    printf '%s\n' "Docker Engine is unavailable. Start Docker and retry." >&2
    exit 1
fi
if ! command -v adb >/dev/null 2>&1; then
    printf '%s\n' "adb was not found on PATH." >&2
    exit 1
fi
if [[ "$(adb -s emulator-5554 get-state 2>/dev/null || true)" != "device" ]]; then
    printf '%s\n' "emulator-5554 is not reachable. Start and boot the demo AVD, then retry." >&2
    exit 1
fi

printf '%s\n' "[OK] Temurin JDK 17.0.21" "[OK] Gemini key is configured (value hidden)" \
    "[OK] Docker Engine is available" "[OK] emulator-5554 is reachable"

dev_compose up -d --wait
if [[ "$(dev_compose exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping' 2>/dev/null)" != "PONG" ]]; then
    printf '%s\n' "Redis did not become healthy after dev-up." >&2
    exit 1
fi
printf '%s\n' "[OK] Redis health check"

(
    cd "$DEV_ROOT/backend"
    ./mvnw --batch-mode --no-transfer-progress -DskipTests package
)

backend_jar="$DEV_ROOT/backend/target/ai-study-mentor-backend-1.0.0-SNAPSHOT.jar"
if [[ ! -f "$backend_jar" ]]; then
    printf '%s\n' "Expected packaged backend JAR was not created." >&2
    exit 1
fi

log_directory="$DEV_ROOT/backend/target/logs"
mkdir -p "$log_directory"
health_url="http://127.0.0.1:${MANAGEMENT_SERVER_PORT}/actuator/health"
health_check() {
    printf 'user = "%s:%s"\n' "$MANAGEMENT_USERNAME" "$MANAGEMENT_PASSWORD" |
        curl --config - --fail --silent --show-error "$health_url" 2>/dev/null |
        grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'
}

if ! health_check; then
    if [[ -f "$log_directory/demo-backend.pid" ]] &&
       kill -0 "$(cat "$log_directory/demo-backend.pid")" 2>/dev/null; then
        printf '%s\n' "A demo backend process is already running but is not healthy. Inspect backend/target/logs." >&2
        exit 1
    fi
    (
        cd "$DEV_ROOT/backend"
        nohup "$JAVA_HOME/bin/java" -Dspring.profiles.active=dev -jar "$backend_jar" \
            >"$log_directory/demo-backend.stdout.log" \
            2>"$log_directory/demo-backend.stderr.log" &
        echo "$!" >"$log_directory/demo-backend.pid"
    )
    ready=0
    for _ in $(seq 1 90); do
        if health_check; then ready=1; break; fi
        if [[ -f "$log_directory/demo-backend.pid" ]] &&
           ! kill -0 "$(cat "$log_directory/demo-backend.pid")" 2>/dev/null; then
            break
        fi
        sleep 2
    done
    if [[ "$ready" -ne 1 ]]; then
        printf '%s\n' "Spring Boot did not become healthy within 180 seconds. Inspect backend/target/logs/demo-backend.*.log." >&2
        exit 1
    fi
fi
printf '%s\n' "[OK] Spring Boot healthy; Flyway migrations and dev seed completed during startup"

(
    cd "$DEV_ROOT/android"
    ./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug
)

apk="$DEV_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$apk" ]]; then
    printf '%s\n' "Android debug APK was not created." >&2
    exit 1
fi
adb -s emulator-5554 install -r "$apk"
adb -s emulator-5554 shell monkey -p com.aistudymentor 1

cat <<'EOF'

Demo environment is ready.
Demo account: demo.middle@local.test / DemoPass123! (local-only public development fixture)
TOTP helper: bash ./scripts/totp.sh (Windows PowerShell: .\scripts\totp.ps1)
Mailpit: http://127.0.0.1:8025
API base: http://10.0.2.2:8080/api/v1/ (from emulator)
Core API routes: /auth/register, /auth/login, /auth/mfa/verify, /profile/me, /questions, /quizzes, /dashboard/metrics, /account
Backend health: management endpoint on 127.0.0.1:8081 (management credentials required)
Backend logs: backend/target/logs/demo-backend.stdout.log and demo-backend.stderr.log
EOF
