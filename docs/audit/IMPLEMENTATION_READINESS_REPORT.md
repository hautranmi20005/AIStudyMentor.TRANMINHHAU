# Implementation Readiness Report

## Readiness summary

**Status: Partially implemented; not ready for a verified Android emulator demonstration.**

Evidence: Android assemble/lint tasks completed but the Gradle invocation failed at shutdown and Android unit tests were `NO-SOURCE`. Backend `verify` passed on Java 25 (31 tests, 4 Docker-dependent skips); it failed under default Java 17 due Java 25 test bytecode. The backend-only H2 registration/login/question probe persisted a question but returned no answer. Docker and `adb` were unavailable, so the requested real MySQL/Redis/Mailpit + emulator flow was not run.

## Blocking decisions and risks

1. **Potential credential exposure:** `backend/.env` is tracked and non-empty credential settings also occur in history. Rotate any live credentials and agree an authorized history-remediation approach before relying on them. Values are intentionally omitted.
2. **Java target approval:** user-provided frozen baseline says Java 17; the current project configuration is Java 25; no Java-version ADR was found.
3. **Missing requirements sources:** the named Product Completion Blueprint and Technical SPEC v2 are absent. FR-09..FR-11 and requested Blueprint AI sections cannot be assessed without them.
4. **Development stack unavailable:** Docker engine and Android emulator/ADB were unavailable during this audit.
5. **Non-functional AI response:** unset Gemini causes normal question submission to return `SUBMITTED` without an answer; fake local solver code and synthetic quiz content are also present.

## Dependency-ordered backlog

Effort estimates are engineering estimates, not approved scope. Items that require a missing decision/spec are marked as gated; do not infer requirements.

| Order | Work item | Depends on | Effort | Readiness / acceptance evidence |
|---:|---|---|---|---|
| 1 | Rotate/revoke any live credentials in the tracked environment file and history; remove secret material from tracked state using an explicitly approved repository-history procedure. | Repository owner/security approval | M | Secret scan re-run with gitleaks/trufflehog; no values in current tree/history; credentials rotated and validated without disclosure. |
| 2 | Reconcile Java 17 frozen baseline versus Java 25 project files and record the approved runtime ADR; align backend POM, container, CI and technical stack. | Product/technical owner decision | S-M | Decision log, build toolchain and CI agree; exact Maven verification passes on the documented JDK. |
| 3 | Provide the missing Blueprint and Technical SPEC v2, including FR-09..FR-11 and AI sections 6.3/6.4/8.3. | Document owner | S | Requirements matrix and AI comparison updated without assumptions. |
| 4 | Enable Docker and Android SDK/emulator on the development machine; run MySQL, Redis and Mailpit local services. | Environment setup | S-M | `docker compose` health checks pass; emulator appears in `adb devices`; API is reachable from `10.0.2.2`. |
| 5 | Restore a clean, reproducible baseline and add meaningful Android unit/instrumented coverage; rerun backend and Android suites without tracked generated-output churn. | 2, 4 | M | Java-consistent backend `verify`; Android unit tests are discovered and pass; lint/build exits 0; emulator smoke evidence saved. |
| 6 | Complete MFA enrollment/activation endpoints and Android enrollment/onboarding, then verify registration/login/refresh/logout and force-stop/reopen token behavior. | 2, 4, missing API decisions if any | L | API/integration tests cover factor enrollment, lockout, rotation/revocation; emulator flow survives relaunch. |
| 7 | Make normal text question submission fail explicitly when Gemini is unconfigured; remove any synthetic answer fallback from the production path. | 2, 3 | M | API test asserts structured `AI_NOT_CONFIGURED`; configured provider integration returns a validated answer; no fabricated answer path. |
| 8 | Implement the specified exact-match answer cache and complete usage telemetry. | 3, 4, 7 | M-L | Redis hit/miss and TTL tests; cache key/version isolation; measured cache-hit response meets the specified threshold; input/output usage populated. |
| 9 | Wire quiz generation to the approved AI design and complete Android quiz create/start/submit/history screens while retaining server-only grading answers. | 3, 4, 7 | L | No generic fixed quiz content; API and UI tests prove client never receives correct answers before submission. |
| 10 | Implement Firebase image upload and backend ownership verification, attachment persistence, and Gemini image requests. | 3, 4, Firebase configuration/adapter decision | L | Authenticated upload-to-answer integration test; cross-user path rejected; image bytes reach provider without exposing storage credentials. |
| 11 | Wire profile fetch/update UI and bookmark add/remove operations to the existing backend endpoints. | 4, 5 | M | Authenticated per-user profile/bookmark integration tests and emulator verification; no hard-coded account data. |
| 12 | Reconcile production database choice and migration/schema support (MySQL design/dev/CI versus PostgreSQL prod). | Database owner decision | M-L | Chosen database matches Flyway migrations, JDBC driver, compose, docs, and integration-test matrix. |
| 13 | Add authorized end-to-end emulator evidence for register -> login -> text question -> answer, offline cache, and force-stop/reopen behavior. | 4-12 | M | Screenshots/API logs in an agreed evidence directory; credentials/tokens redacted; all tested steps pass. |

## Scope cautions

- Search, report, and delete-account have no endpoint evidence and are not specified in the available SRS. Do not add them until their requirements are supplied and approved.
- Forgot-password is likewise not specified in the available SRS.
- The decision log does not contain the ADD-03/ADD-04 adapter decisions referenced in task context; obtain the authoritative text before implementing local Firebase/FCM adapters.
- Do not claim production readiness: the SRS itself marks the under-13 consent flow as pending legal review.
