#!/usr/bin/env bash
set -euo pipefail
script_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$script_dir/dev-common.sh"
load_dev_environment
base_url="${1:-http://127.0.0.1:8080/api/v1}"
demo_email="${DEMO_EMAIL:-demo.middle@local.test}"
demo_password="$DEV_SEED_PASSWORD"
email="smoke.$(python3 -c 'import uuid; print(uuid.uuid4().hex)')@local.test"
password="LocalTest123!"
registration="$(printf '{"email":"%s","password":"%s","dob":"2000-01-01","educationLevel":"University"}' "$email" "$password")"
status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --header 'Content-Type: application/json' --data "$registration" "$base_url/auth/register")"
[[ "$status" == "201" ]] || { printf 'Registration failed: HTTP %s\n' "$status" >&2; exit 1; }

login_body="$(printf '{"email":"%s","password":"%s"}' "$email" "$password")"
curl --silent --show-error --fail --header 'Content-Type: application/json' \
    --data "$login_body" "$base_url/auth/login" >/dev/null

demo_body="$(printf '{"email":"%s","password":"%s"}' "$demo_email" "$demo_password")"
demo_response="$(curl --silent --show-error --fail --header 'Content-Type: application/json' \
    --data "$demo_body" "$base_url/auth/login")"
mfa_token="$(printf '%s' "$demo_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["mfaSessionToken"])')"
code="$("$script_dir/totp.sh" "$DEV_SEED_TOTP_SECRET")"
verify_body="$(printf '%s\n%s\n' "$mfa_token" "$code" | python3 -c 'import json,sys; token,code=sys.stdin.read().splitlines(); print(json.dumps({"mfaSessionToken":token,"code":code,"type":"TOTP"}))')"
verified_response="$(curl --silent --show-error --fail --header 'Content-Type: application/json' \
    --data "$verify_body" "$base_url/auth/mfa/verify")"
printf '%s' "$verified_response" | python3 -c 'import json,sys; assert json.load(sys.stdin).get("accessToken")'

printf '[OK] Register: HTTP %s\n' "$status"
printf '%s\n' '[OK] Login for new account' '[OK] Demo MFA login and TOTP verification'
