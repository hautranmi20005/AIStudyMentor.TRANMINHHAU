#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/dev-common.sh"
load_dev_environment
failed=0

check() {
    local name="$1"
    shift
    if "$@" >/dev/null 2>&1; then
        printf '[OK]   %s\n' "$name"
    else
        printf '[FAIL] %s\n' "$name"
        failed=1
    fi
}

check "MySQL" dev_compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -uroot --silent -Nse "SELECT 1"'
check "Redis" dev_compose exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
check "Mailpit" curl --fail --silent --show-error http://127.0.0.1:8025/livez
check "Backend actuator health" bash -c \
    'printf "user = \"%s:%s\"\n" "$1" "$2" | curl --config - --fail --silent --show-error "http://127.0.0.1:$3/actuator/health" | grep -q "\"status\":\"UP\""' \
    _ "$MANAGEMENT_USERNAME" "$MANAGEMENT_PASSWORD" "$MANAGEMENT_SERVER_PORT"

exit "$failed"
