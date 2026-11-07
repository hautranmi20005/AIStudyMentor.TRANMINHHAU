# Product Requirements Document (PRD) v1.2

## 1. Product Vision
Empower students with instant, safe, and structured academic assistance powered by Google Gemini, designed with an Android-first native experience.

## 2. Core User Journeys
1. **Onboarding & Security**: Register (with age validation), setup TOTP MFA, and complete Under-13 guardian consent email verification if under 13.
2. **Q&A**: Ask academic questions via text or CameraX photo upload, get step-by-step solutions formatted in Material 3 cards.
3. **Practice**: Generate custom quizzes, solve them on a distraction-free UI, and receive instant server-side grading.
4. **Dashboard**: Track subject mastery, accuracy trends, and study volume.

## 3. Product Decisions & Technical Status
* **MFA Factor (Resolved - Decision D1)**: Standard TOTP (RFC 6238) via Authenticator apps. "Resend code" concept eliminated.
* **Guardian Verification Method (Resolved - Decision D2)**: 72-hour signed single-use email loop with SHA-256 token hash and IP audit logging. *(Note: legal review pending - not production-ready)*.
* **Account Status Hierarchy (Resolved - Decision D6)**: `ACTIVE`, `PENDING_CONSENT`, `SUSPENDED`.
* **Gemini Model ID & Version (TBD - Decision D3)**: Configured as placeholder `<SET_ME>`. AI requests fail fast with `AI_NOT_CONFIGURED` until PO provides final production model identifiers.
