# Agent Collaboration Rules v1.2

## 1. Ownership & Package Boundaries

1. **Orchestrator Agent**:
   - Decomposes requirements, enforces architectural integrity, approves API contracts, and manages the project changelog.
   - **Authority Boundary**: All TBD decisions, model selections, and policy changes belong strictly to the **Human Project Owner (PO)**. The Orchestrator cannot make architectural assumptions without PO approval.

2. **Android Lead Agent**:
   - Owns `/android/**`.
   - Responsible for Jetpack Compose UI, Material 3 theming, ViewModels, StateFlow, Room local database, and Retrofit network client.

3. **Backend Lead Agent**:
   - Owns `backend/src/main/java/com/aistudymentor/{auth, profile, question, quiz, config, common}/**`.
   - Owns `backend/src/main/resources/**` (including Flyway migrations `db/migration/V*`).
   - Owns `backend/pom.xml`.
   - Responsible for Spring Security, JWT issuance, database entities, repositories, and server-side grading.

4. **AI/Data Lead Agent**:
   - Strictly owns `backend/src/main/java/com/aistudymentor/ai/**` and `docs/ai_response.schema.json`.
   - Responsible for Google Gemini REST integration, system prompt engineering, Redis exact-match cache logic, and AI response validation.

5. **QA / Infra Lead Agent**:
   - Owns `backend/docker-compose.yml` and `backend/.env.example`.
   - Owns `backend/src/test/**` (Unit tests, Testcontainers, Integration tests).
   - Owns Firebase Security Rules and Android Emulator connectivity setup (`10.0.2.2`).

---

## 2. Immutable Rules

* **Frozen Tech Stack**: No agent may change the approved tech stack (Spring Boot, Kotlin/Compose, MySQL 8, Redis 7, Flyway, Gemini) without explicit human PO approval.
* **No Client-Side Scoring**: Quiz correct answers must never be present in client DTOs or sent to the Android app before submission.
* **Zero Secrets Exposure**: API keys (Gemini, JWT, MFA encryption) must never be committed or included in client code.
* **Flyway Immutability**: Never mutate existing Flyway migration files. All schema evolutions must be introduced as new numbered versions (`V2__`, `V3__`, etc.).
