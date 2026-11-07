# System Architecture v1.1

## 1. Architecture Diagram
```mermaid
flowchart TB
  A[Android App<br/>Kotlin + Compose] -->|HTTPS REST| B[Spring Boot API Monolith]
  A -->|Authenticated Upload| F[Firebase Storage]
  
  B --> C[Auth, MFA & Consent Module]
  B --> D[Question & Quiz Module]
  B --> E[AI Orchestrator Module]
  B --> P[Progress Dashboard Module]
  
  B --> G[(MySQL)]
  E --> H[(Redis Cache)]
  E --> I[Gemini API Adapter]
  I --> J[Google Gemini]
```

## 2. Data Flow (Image Question)
1. App gets upload authorization/token.
2. App uploads image directly to Firebase Storage.
3. App sends `/api/v1/questions` request with the Firebase object path.
4. Backend verifies ownership, fetches image bytes, and calls Gemini via adapter.
