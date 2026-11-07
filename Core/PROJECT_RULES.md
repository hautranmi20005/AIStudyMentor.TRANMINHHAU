# Project Rules v1.1

## 1. Frozen Technology Decisions
- Android: Kotlin, Compose, MVVM, Retrofit, Room, Hilt.
- Backend: Java, Spring Boot, Spring Security, Spring Data JPA, Flyway.
- Database: MySQL (Relational), Redis (Cache), Firebase (Image Storage & FCM).
- AI: Google Gemini (Backend integration only).

## 2. Constraints
- **NO API KEYS IN ANDROID**: The Gemini API key must reside purely in backend environment variables.
- **NO CLIENT-SIDE SCORING**: Quiz correct answers must never be sent to the Android client until the attempt is submitted.
- **MODULAR MONOLITH**: Microservices are explicitly banned for this MVP.
- **OWNERSHIP**: Every DB resource must belong to a verified profile.
