#!/usr/bin/env bash

DEV_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DEV_ENV_FILE="$DEV_ROOT/backend/.env"

load_dev_environment() {
    if [[ ! -f "$DEV_ENV_FILE" ]]; then
        "$DEV_ROOT/scripts/gen-secrets.sh"
    fi
    set -a
    # This file is generated from the repository's controlled KEY=value template.
    # shellcheck disable=SC1090
    source "$DEV_ENV_FILE"
    set +a
}

dev_compose() {
    docker compose --project-name aistudymentor-dev --env-file "$DEV_ENV_FILE" \
        -f "$DEV_ROOT/docker-compose.dev.yml" "$@"
}
