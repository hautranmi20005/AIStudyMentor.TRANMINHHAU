# Software Requirements Specification (SRS)

## Functional Requirements
- **FR-01A**: User Registration/Login
- **FR-01B**: Student Profile Management
- **FR-01C**: Multi-Factor Authentication (MFA)
- **FR-01D**: Under-13 Guardian Consent Workflow
- **FR-02**: Text Question Submission
- **FR-03**: Photo Question Submission (Firebase Storage)
- **FR-04**: Structured AI Answer via Gemini
- **FR-05**: Alternative Explanations
- **FR-06A**: Question History
- **FR-06B**: Bookmarks
- **FR-07**: Practice Quiz Generation & Server-side Scoring
- **FR-08**: Progress Dashboard (Metrics aggregation)

## Non-Functional Requirements
- **NFR-01**: AI answers must return strictly validated JSON.
- **NFR-02**: Exact-match cache (Redis) must respond in <200ms.
- **NFR-03**: Android UI must be implemented in Jetpack Compose.
- **NFR-04**: AI credentials must not reside in the APK.
