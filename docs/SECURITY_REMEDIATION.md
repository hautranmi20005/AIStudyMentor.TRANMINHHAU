# Security remediation: committed credentials and repository hygiene

## Current action

The committed `backend/.env` values are treated as compromised. The file is removed from the Git index but retained locally, then replaced with fresh local-only values using `scripts/gen-secrets.ps1` or `scripts/gen-secrets.sh`. The repository does not rewrite history or force-push as part of this task.

The generator creates:

- A JWT signing secret from 64 random bytes, Base64-encoded.
- An MFA encryption key of exactly 32 ASCII bytes.
- New local MySQL, Redis, PostgreSQL, and management passwords.

Generating a new `.env` does not change credentials already stored in an initialized database volume or external service. Rotate the database account through an approved database procedure; do not delete existing volumes as a shortcut.

Provider credentials are not generated locally. If a real Gemini or SendGrid key was ever stored in the committed environment file, revoke it at the provider and issue a replacement through the provider console. Never put the replacement in a tracked file.

## Credentials to rotate

1. **Database credentials**: replace any database account password that reused a committed `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `POSTGRES_PASSWORD`, or equivalent. This includes shared/staging/production databases only if those values were used there. The generator's new database passwords are for this local checkout and are not an external account rotation.
2. **Redis credentials**: replace any Redis password that reused a committed `REDIS_PASSWORD` or equivalent, including hosted instances if applicable.
3. **JWT signing secret**: replace it in every environment where the committed value was used. This invalidates existing access/refresh tokens; users may need to sign in again.
4. **MFA encryption key**: replace it in every environment where the committed value was used. Existing encrypted MFA factors cannot be decrypted with a new key. Before rotation in a shared environment, coordinate secure recovery/re-enrollment or an approved decrypt-and-re-encrypt migration. Do not retain the compromised key in source control or expose it during migration.
5. **Gemini and SendGrid keys**: revoke and reissue any real provider keys found in the prior committed file; update only protected local/deployment secret stores.
6. **Management credentials**: rotate any management endpoint username/password reused outside this local setup.

Treat all previous values as compromised even if a provider-format scanner did not recognize them. Check provider/database audit logs for suspicious use and review deployments for copied values.

## Optional history cleanup (not performed)

Removing files from the current index does not erase their historical Git blobs. If the team later approves history cleanup:

1. Coordinate a maintenance window with **all four team members** and the repository owner. Pause merges, pushes, releases, and automated deployments; notify anyone with forks or clones.
2. Rotate/revoke affected credentials first. History rewriting is not a substitute for rotation.
3. Choose one maintainer to create and verify a protected backup, then use an approved `git filter-repo` or BFG procedure to remove `backend/.env` and generated/IDE artifacts from every relevant ref.
4. Review the rewritten refs and secret scan results before publishing. Any force-push must be separately authorized; none is part of this task.
5. Require all four members to discard old clones and re-clone (or follow a coordinated clean-ref procedure). Old commits can reintroduce the removed files if pushed.
6. Re-run CI, confirm tags/releases/forks and deployment references are handled, and record the completion and remaining exceptions.

## Ongoing controls

- `.gitignore` excludes local environment files, build outputs, IDE metadata, `android/local.properties`, and Windows shortcut files. Ignore rules do not untrack files already in Git; the index removals must be committed separately.
- `backend/.env.example` and `android/local.properties.example` contain configuration placeholders only. Do not put credentials in examples.
- CI runs Gitleaks against the source tree. Its configuration excludes only generated/IDE paths and the ignored local `backend/.env`; it does not scan Git history.
- Android `local.properties` contains only machine-local settings such as the optional `API_BASE_URL`; it must not contain credentials.
