# Database Design

The development backend uses MySQL with Flyway-managed, forward-only migrations V1–V16. Production configuration targets PostgreSQL, but the PostgreSQL SQL migration directory lacks V7, V8, V10, and V11, and `application-prod.yml` does not include the Java-migration package used for V9. The production schema path is incomplete and has not been verified. The `docs/API_SPEC.md` inventory records current controller behavior, which is not identical to every table present in the schema.

## Identity and onboarding

| Table | Identity-related data | Relationship and constraints |
|---|---|---|
| `users` | User identity, BCrypt password hash, account status, date of birth, and `auth_version` | `user_id` is the account key. Incrementing `auth_version` invalidates previously issued access, MFA-session, and refresh tokens. |
| `student_profiles` | Education level, preferred explanation style, and `onboarding_completed` | One profile belongs to a user. |
| `student_profile_subjects` | Ordered subject names for a profile | Composite primary key `(profile_id, subject_order)`; deleting the profile cascades to its subjects. |
| `mfa_factors` | Encrypted TOTP secret and verification state | A factor belongs to a user; the secret is not stored as plaintext. |
| `mfa_recovery_codes` | BCrypt hash for each one-time recovery code and creation time | User deletion cascades to codes; a successfully used code is consumed. Raw codes are returned only during generation/regeneration. |
| `password_reset_tokens` | SHA-256 token hash, expiry, and creation time | Token hash is unique; user deletion cascades to tokens. Raw reset tokens are sent to the user and are not persisted. |
| `guardian_consents` | Guardian-consent state and token metadata | Supports pending consent for restricted accounts; token state and expiry are managed by the guardian-consent flow. |

## Identity data lifecycle

- Password changes increment `users.auth_version` and remove stored reset tokens, invalidating existing sessions.
- A recovery-code regeneration replaces previously stored codes; each code can succeed at most once.
- Password reset tokens are single-use and expire after 60 minutes.
- Foreign-key cascades protect relational child records when a user or profile is removed. External image cleanup and account-deletion APIs are not part of this identity migration.

## Migration notes

V5 adds `users.auth_version`, `student_profiles.onboarding_completed`, the ordered `student_profile_subjects` collection, hashed MFA recovery codes, and hashed password-reset tokens. It does not modify V1–V4. The integration suite validates the Flyway schema against MySQL Testcontainers and exercises identity persistence flows.

## Migration inventory

| Version | Schema change |
|---|---|
| V1 | Core users, MFA factors, guardian consent, and student profiles |
| V2 | Account/MFA/consent constraints, guardian-consent token and audit fields, MFA-attempt records |
| V3 | Questions, attachments, answers, AI-generation logs, and bookmarks |
| V4 | Practice quizzes, questions/options, attempts, and responses |
| V5 | Identity lifecycle, onboarding subjects, recovery codes, password-reset tokens |
| V6 | AI usage metadata and quiz-question topic metadata |
| V7 | `answer_reports` table and indexes; no report API controller is present |
| V8 | Private image-upload metadata and attachment size/hash columns |
| V9 | Java migration for history indexes and `question_sync_deletions`; its PostgreSQL implementation is in `db.postgresqlmigration`, but the production Flyway locations do not currently include that package |
| V10 | Quiz source/type/answer schema extensions |
| V11 | Dashboard and quiz aggregation indexes |
| V12 | Bookmark last-view time, device tokens, notification preferences/deliveries, and indexes |
| V13 | Resumable account-deletion jobs/audit and retained guardian-consent evidence |
| V14 | Removes redundant profile and answer fields |
| V15 | Adds the profile cascade foreign key to `question_sync_deletions` |
| V16 | Allows `DELETION_PENDING` as a user account status |

The presence of schema objects does not imply a complete API or Android flow. In particular, V7's report table is not exposed by a controller; V8's upload metadata is not connected to an Android upload flow; and V9's tombstones are not exposed through a `/sync/history` controller in this checkout.

## Account and content lifecycle

`AccountPrivacyService` requires password and MFA reauthentication, records a deletion job, marks the account `DELETION_PENDING`, removes user-scoped storage and Redis keys, deletes the account, and records a hashed deletion audit. The operation can resume from a pending job after an external cleanup failure. Foreign keys cascade relational child rows; external storage cleanup is handled by the service rather than by a database cascade.

Current schema additions include notification/device-token tables (V12), deletion/consent-evidence tables (V13), and image-upload metadata (V8). The report and image tables are not evidence of working client journeys; see [API_SPEC.md](./API_SPEC.md).
