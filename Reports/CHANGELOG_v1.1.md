# Changelog v1.1 (2026-11-07)

## Added
- **MFA Specifications**: Documented MFA enrollment, challenge, and verification flows. Added `mfa_factors` DB entity and `/api/v1/auth/mfa` APIs.
- **Under-13 Guardian Consent**: Added privacy/consent workflow, `guardian_consents` DB entity, and strict authorization blocks for pending accounts.
- **Extended MVP (Quizzes & Dashboard)**: Formalized FR-07 and FR-08. Added practice quizzes, attempts, grading logic, and progress telemetry.
- **Agent Personas**: Generated missing Agent documentation for AI/Data Lead, Android Lead, Backend Persona, QA Persona.
- **Validation Reports**: Added Source Audit, Conflict, and Validation reports.

## Changed
- **AI Provider**: Officially set to Google Gemini. Refactored `AI_ORCHESTRATOR_SPEC.md` to reference Gemini specific constraints.
- **MVP Scope**: Promoted Practice Quizzes and Progress Dashboard from 'Conditional' to 'Core Extended MVP'.
- **Database Design**: Expanded schema to include Quizzes, Attempts, MFA, and Consent logs.
