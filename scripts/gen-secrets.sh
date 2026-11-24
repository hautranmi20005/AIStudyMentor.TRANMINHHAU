#!/usr/bin/env bash
set -euo pipefail
umask 077

repository_root="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
example_path="$repository_root/backend/.env.example"
environment_path="$repository_root/backend/.env"

if [[ ! -f "$example_path" ]]; then
    printf '%s\n' "Required template not found: backend/.env.example" >&2
    exit 1
fi

overwrite=0
if [[ "${1:-}" == "--overwrite" ]]; then
    overwrite=1
elif [[ $# -gt 0 ]]; then
    printf '%s\n' "Usage: $0 [--overwrite]" >&2
    exit 2
fi

if [[ -e "$environment_path" && "$overwrite" -eq 0 ]]; then
    printf '%s' "backend/.env exists. Replace it with newly generated local credentials? Type YES to continue: "
    IFS= read -r answer
    if [[ "$answer" != "YES" ]]; then
        printf '%s\n' "Existing backend/.env was not changed." >&2
        exit 1
    fi
fi

if ! command -v openssl >/dev/null 2>&1; then
    printf '%s\n' "OpenSSL is required to generate local credentials." >&2
    exit 1
fi

JWT_SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
MFA_ENCRYPTION_KEY="$(openssl rand -base64 24 | tr -d '\r\n')"
DB_PASSWORD="$(openssl rand -base64 32 | tr -d '\r\n')"
DB_ROOT_PASSWORD="$(openssl rand -base64 32 | tr -d '\r\n')"
REDIS_PASSWORD="$(openssl rand -base64 32 | tr -d '\r\n')"
MANAGEMENT_PASSWORD="$(openssl rand -base64 32 | tr -d '\r\n')"

temporary_path="$(mktemp "$repository_root/backend/.env.tmp.XXXXXX")"
trap 'rm -f -- "$temporary_path"' EXIT

JWT_COUNT=0
MFA_COUNT=0
DB_COUNT=0
DB_ROOT_COUNT=0
REDIS_COUNT=0
MANAGEMENT_COUNT=0

while IFS= read -r line || [[ -n "$line" ]]; do
    case "$line" in
        JWT_SECRET=*) printf 'JWT_SECRET=%s\n' "$JWT_SECRET"; JWT_COUNT=$((JWT_COUNT + 1)) ;;
        MFA_ENCRYPTION_KEY=*) printf 'MFA_ENCRYPTION_KEY=%s\n' "$MFA_ENCRYPTION_KEY"; MFA_COUNT=$((MFA_COUNT + 1)) ;;
        DB_PASSWORD=*) printf 'DB_PASSWORD=%s\n' "$DB_PASSWORD"; DB_COUNT=$((DB_COUNT + 1)) ;;
        DB_ROOT_PASSWORD=*) printf 'DB_ROOT_PASSWORD=%s\n' "$DB_ROOT_PASSWORD"; DB_ROOT_COUNT=$((DB_ROOT_COUNT + 1)) ;;
        REDIS_PASSWORD=*) printf 'REDIS_PASSWORD=%s\n' "$REDIS_PASSWORD"; REDIS_COUNT=$((REDIS_COUNT + 1)) ;;
        MANAGEMENT_PASSWORD=*) printf 'MANAGEMENT_PASSWORD=%s\n' "$MANAGEMENT_PASSWORD"; MANAGEMENT_COUNT=$((MANAGEMENT_COUNT + 1)) ;;
        *) printf '%s\n' "$line" ;;
    esac
done < "$example_path" > "$temporary_path"

if [[ "$JWT_COUNT" -ne 1 || "$MFA_COUNT" -ne 1 || "$DB_COUNT" -ne 1 ||
      "$DB_ROOT_COUNT" -ne 1 || "$REDIS_COUNT" -ne 1 || "$MANAGEMENT_COUNT" -ne 1 ]]; then
    printf '%s\n' "Template is missing or duplicates a required secret key; no output file was changed." >&2
    exit 1
fi

chmod 600 "$temporary_path"
mv -f -- "$temporary_path" "$environment_path"
trap - EXIT

printf '%s\n' "Generated backend/.env with fresh local credentials. Secret values were not displayed."
