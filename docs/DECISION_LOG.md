# AI Study Mentor Decision Log

This index records the approved P03 architecture decisions and links to their individual ADRs. “Approved” describes the selected architecture policy; each ADR separately records whether the current implementation matches that policy.

The older [Core decision log](../Core/DECISION_LOG.md) remains available as a historical project register. Its ADR numbering predates this P03 ADR series.

| ADR | Decision | Status | Implementation alignment |
|---|---|---|---|
| [ADR-0001](./adr/ADR-0001-gemini-multimodal-flash-model-configuration.md) | Gemini multimodal Flash model configuration | Approved | Not aligned: hard-coded model defaults and a local smart-solver implementation remain. |
| [ADR-0002](./adr/ADR-0002-totp-mfa-and-recovery-codes.md) | TOTP MFA and one-time recovery codes | Approved | Backend enrollment, challenge, hashed one-time recovery codes, disable/regenerate flows, and MFA rate-limit tests are implemented; end-to-end live emulator verification remains outstanding. |
| [ADR-0003](./adr/ADR-0003-under-13-guardian-consent.md) | Under-13 guardian consent and verification email | Approved | Partial: token flow exists; legal review remains outstanding. |
| [ADR-0004](./adr/ADR-0004-mysql-relational-database-baseline.md) | MySQL relational database baseline | Approved | Not aligned: PostgreSQL production configuration and artifacts remain. |
| [ADR-0005](./adr/ADR-0005-redis-exact-match-answer-cache.md) | Redis exact-match answer cache | Approved | Not aligned: cache/key/orchestrator components exist, but `QuestionService` bypasses them and records `cacheHit=false`; repeated-question hits are not implemented on the active path. |
| [ADR-0006](./adr/ADR-0006-server-side-quiz-generation-and-grading.md) | Server-side quiz generation and deterministic grading | Approved | Implemented: `QuizService` delegates generation to `AiOrchestrator`, validates generated content, keeps answer keys server-side, and supports pre-attempt subject edit/delete. Mocked-provider integration and Android UI tests pass; live Gemini behavior remains unverified. |
| [ADR-0007](./adr/ADR-0007-private-image-storage-and-signed-upload-authorization.md) | Private image storage and signed upload authorization | Approved | Not aligned: storage service classes and V8 metadata exist, but the active question controller only returns a generated path/token; byte upload and Android image-question paths are absent. |
| [ADR-0008](./adr/ADR-0008-deterministic-progress-analytics.md) | Deterministic, timezone-aware progress analytics | Approved | Not aligned: `DashboardService` averages quiz-attempt scores and returns totals/by-subject counts; it has no range/timezone filters or detailed time-series aggregates. |
| [ADR-0009](./adr/ADR-0009-data-retention-and-account-deletion-cascade.md) | Data retention and account-deletion cascade | Approved | Partial: reauthenticated, resumable account deletion and external-storage cleanup exist; legal retention policy and production operation are not established. |
| [ADR-0010](./adr/ADR-0010-idempotent-bookmarks-and-question-submissions.md) | Idempotent bookmarks and question submissions | Approved | Partially aligned: POST adds and DELETE removes bookmarks idempotently; bookmark creation still uses POST rather than the ADR's PUT, and question creation has no idempotency key. |

P19 code-to-documentation inspection is recorded in [KNOWN_LIMITATIONS.md](./KNOWN_LIMITATIONS.md) and the [final acceptance checklist](./evidence/final/ACCEPTANCE_CHECKLIST.md). These alignment statements describe the current source, not a production deployment or a completed live demo.
