# Staging Deployment and Android Beta Distribution

## Backend staging stack

`backend/docker-compose.prod.yml` runs the Spring Boot backend, PostgreSQL, and password-protected Redis on a private Compose network. PostgreSQL and Redis data are stored in named volumes. Only the backend port is published, bound to `127.0.0.1` by default; place a TLS reverse proxy in front of it for remote access. The database and cache ports are not published to the host.

This Compose stack is intended for staging or a single-host deployment. PostgreSQL and Redis are single instances; use managed, replicated services for high availability and disaster recovery. Back up the named volumes and database separately. Avoid deleting volumes during routine deployments.

### Configure and start

1. Copy `backend/.env.prod.example` to `backend/.env.prod` and replace every placeholder with unique, high-entropy secrets. Keep this file out of Git.
2. Set `APP_BASE_URL` to the public HTTPS backend URL, `CORS_ALLOWED_ORIGINS` to exact browser origins (comma-separated, no wildcard), and the verified SendGrid sender/API key. Android native clients do not require CORS.
3. Check and start the stack from the repository root:

   ```powershell
   docker compose --env-file backend/.env.prod -f backend/docker-compose.prod.yml config --quiet
   docker compose --env-file backend/.env.prod -f backend/docker-compose.prod.yml up -d --build
   ```

4. Check status and logs:

   ```powershell
   docker compose --env-file backend/.env.prod -f backend/docker-compose.prod.yml ps
   docker compose --env-file backend/.env.prod -f backend/docker-compose.prod.yml logs -f backend
   ```

The backend waits for PostgreSQL and Redis health checks, applies the PostgreSQL Flyway migrations, and runs as a non-root user with a read-only root filesystem. To use managed database/cache services instead, provide their connection values through the corresponding Spring environment variables and deploy the backend separately; do not expose credentials in Compose files.

## Firebase beta distribution

The workflow at `.github/workflows/deploy-beta.yml` runs Android lint and unit tests, signs a minified release AAB, and uploads it to Firebase App Distribution. It runs on pushes to `staging`, tags matching `beta-*`, or manual dispatch. The AAB is also retained as a GitHub Actions artifact for 14 days.

### GitHub configuration

Create a GitHub Environment named `staging`, then add these environment **secrets**:

- `ANDROID_KEYSTORE_BASE64` — base64 encoding of the Android upload keystore used for the app's release signing identity.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `FIREBASE_GOOGLE_SERVICES_JSON_BASE64` — base64-encoded `google-services.json` for the Firebase Android app. It contains public app identifiers, but CI injects it temporarily and removes it after the job.
- `FIREBASE_SERVICE_ACCOUNT_JSON` — service-account JSON with the Firebase App Distribution Admin role, scoped to the staging Firebase project.

Add these environment **variables**:

- `FIREBASE_ANDROID_APP_ID` — Firebase Android App ID registered for package `com.aistudymentor`.
- `FIREBASE_TESTER_GROUPS` — comma-separated App Distribution group aliases, for example `internal-beta`.
- `STAGING_API_BASE_URL` — the HTTPS URL for the staging API, including `/api/v1/`.

Use the same long-lived upload keystore for all builds intended to update the same Play/Firebase app identity. Never generate a new upload key for an existing app. Rotate credentials only through a planned key-management process. The workflow writes credentials to runner-temporary files, does not print them, and removes them at job completion.

Firebase App Distribution must be enabled for the registered app, and the tester group must exist. Testers need to accept their Firebase invitation and use the Firebase App Tester app to install distributed AAB releases.

## Firebase app monitoring

The Android app includes Firebase Analytics and Crashlytics SDKs. Firebase Gradle plugins activate only when `android/app/google-services.json` exists; CI writes that file temporarily from `FIREBASE_GOOGLE_SERVICES_JSON_BASE64`. Local Firebase-enabled builds can use the Firebase Console's `google-services.json` in the same path; keep it out of Git.

Analytics and Crashlytics collection start disabled. After successful authentication, collection is enabled only when guardian consent is not pending; logout or pending consent disables it. Telemetry uses an ephemeral session ID and fixed operation/error categories. It does not send emails, tokens, student prompts, image content, or response bodies. Retrofit transport/server/JSON parsing failures are recorded as non-fatal Crashlytics events.
