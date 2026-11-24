# P19 Backend Startup Evidence

## Diagnosis

The initial bootstrap failed while creating `jpaSharedEM_entityManagerFactory`. Docker itself was healthy: MySQL, Redis, and Mailpit were all healthy, and ports 8080/8081 had no prior listeners.

The local `backend/.env` did not match the dev Compose configuration:

- `DB_URL` had an invalid JDBC scheme and targeted host port 3306, while the running Compose MySQL port was 13306.
- `DB_USERNAME` did not match Compose's `MYSQL_USER` (`studymentor_app`).
- Stale `SPRING_DATASOURCE_*` variables overrode the dev profile; `SPRING_DATASOURCE_URL` targeted PostgreSQL on port 5432, for which this Compose stack has no service.
- Stale `SPRING_DATA_REDIS_*` variables overrode the valid dev Redis settings with host port 6379 and an empty password, while Compose exposes Redis on 16379 and requires authentication.

These configuration mismatches explain the startup failure; it was not a JPA entity-manager conflict. No password, secret, token, or API key is reproduced here.

## Correction and verification

The local `.env` was corrected without changing or displaying secret values: `DB_URL` now targets `jdbc:mysql://127.0.0.1:13306/ai_study_mentor`, `DB_USERNAME` matches Compose, and the conflicting production-style Spring datasource/Redis overrides were removed. The existing volume data was preserved.

After the correction:

- MySQL and Redis authenticated checks passed; all three Compose services reported healthy.
- Ports 8080 and 8081 were free before launch.
- `.\scripts\demo-up.ps1` completed. Maven packaging succeeded with tests skipped; the backend actuator returned `UP`; the required Gradle debug build succeeded; `adb install -r` succeeded; and the app launch command completed on `emulator-5554`.
- A follow-up `dev-status.ps1` check reported MySQL, Redis, Mailpit, and backend actuator health as OK. A direct authenticated actuator request returned `UP`.

This verifies the demo bootstrap and backend readiness, not the full walkthrough. No registration, question, quiz, consent, or account-deletion journey was executed, and no new core-flow screenshot or screen recording was captured.

The local MySQL `flyway_schema_history` query showed V1–V16 successful. The observed rows are in [flyway-history.txt](./flyway-history.txt).
