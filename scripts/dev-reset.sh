#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/dev-common.sh"
load_dev_environment

if [[ "${1:-}" != "--force" ]]; then
    printf '%s' "This deletes all local MySQL, Redis, and Mailpit data. Type RESET to continue: "
    IFS= read -r confirmation
    if [[ "$confirmation" != "RESET" ]]; then
        printf '%s\n' "Reset cancelled." >&2
        exit 1
    fi
fi

dev_compose down --volumes
"$DEV_ROOT/scripts/dev-up.sh"
printf '%s\n' "Starting the backend now runs Flyway migrations. Press Ctrl+C to stop the backend (services remain running)."
exec "$DEV_ROOT/scripts/dev-backend.sh"
