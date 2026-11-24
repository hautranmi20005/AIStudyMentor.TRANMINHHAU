# Privacy implementation and legal-review notes

This document describes current implementation behavior and open questions. It is not legal advice and does not claim compliance with any law, regulation, or platform policy. Qualified privacy counsel must review the product, age-gating, consent, deletion, retention, notices, and deployment controls before release.

## Implemented

- `DELETE /api/v1/account` requires an authenticated session plus password and verified TOTP re-authentication. Wrong credentials or missing MFA do not start deletion.
- Deletion is resumable: a pending job records the necessary account/profile identifiers and image paths, invalidates sessions, removes stored images and user/profile Redis keys, then deletes the relational account graph and writes a hash-and-timestamp audit entry. The endpoint returns `204` when complete; retries and the scheduled worker resume pending cleanup.
- Logout-all-devices also requires password and TOTP, invalidates sessions using the account auth version, and removes registered device tokens.
- Account deletion preserves only the configured pseudonymous guardian-consent evidence (hash, status, verification timestamp, and optional expiry) plus the minimal deletion audit hash and timestamp. The account deletion job is transient and is removed after completion.
- Scheduled cleanup handles expired password-reset tokens, expired guardian evidence, unattached image uploads, and legacy persistent rate-limit/idempotency keys. Intervals, orphan-upload age, idempotency TTL, and guardian-evidence retention are configurable.
- Android provides privacy/account controls, typed deletion confirmation and re-authentication, logout-all-devices, and post-deletion clearing of Room data, secure session storage, account WorkManager work, image cache, and the FCM token when Firebase is configured.
- The app's current under-13 collection is limited to account/age and guardian-consent data plus study data after consent; analytics/crash SDK instrumentation is disabled. See [the data map](./PRIVACY_DATA_MAP.md) for field and storage details.

## Decisions requiring qualified legal review

- **Guardian evidence retention:** `GUARDIAN_CONSENT_EVIDENCE_RETENTION_DAYS` currently defaults to `0`, meaning no expiry. Decide and document the lawful purpose, access controls, retention duration, and deletion process before production. A SHA-256 user-ID hash is pseudonymous data, not anonymous data.
- **Under-13 guardian action — Known Limitation:** an account holder can delete the account, but a guardian does not have a supported independent mechanism to revoke consent or request deletion after approval. Guardian-initiated revocation/deletion must be designed or explicitly accepted as a known limitation before launch.
- **Deletion and backups:** account deletion removes live relational data, external image objects, and targeted Redis data. Backup expiration, point-in-time recovery copies, provider logs, and email-provider copies are governed by deployment/provider retention and are not immediately purged by the endpoint. Publish and enforce an approved backup-retention period.
- **Data protection at rest:** TOTP secrets use AES-GCM; Android session tokens use encrypted shared preferences. Learning content and most relational fields are not field-encrypted by the application. Verify infrastructure encryption, key custody, access boundaries, and backups in each environment. Confirm whether ADD-06 requires a wider field-level encryption scope.
- **Key lifecycle:** `MFA_ENCRYPTION_KEY` is environment-supplied. Key versioning and automated rotation are not implemented. Use the staged rotation procedure in the [data map](./PRIVACY_DATA_MAP.md) and validate existing production key size before deploying strict key validation.
- **Identity, notices, and consent:** review age determination, guardian verification evidence, email notices, consent language, withdrawal workflow, and service availability/retention with counsel for every intended jurisdiction.
- **Audit policy:** confirm whether retaining a non-expiring hash-and-timestamp deletion receipt is necessary and proportionate, who can access it, and how long it may remain.
- **Operational retention:** configure application/provider logs, backups, Redis, object storage, and email-provider retention to the approved policy. Confirm that deletion requests are honored across any third-party processors and replicas.

## Transport security

Android release builds require HTTPS API URLs and cleartext is disallowed by the main network security configuration. Debug builds allow local HTTP for emulator/loopback development. Therefore local development is HTTP-only; production endpoints, storage, and mail links must use TLS. Verify certificates and deployment ingress separately.

## Decision reference

DEC-04 requires legal review; DEC-12 / PD-007 selects hard deletion for the MVP. These implementation choices do not establish legal sufficiency. A release decision should be recorded only after qualified review of the open items above.
