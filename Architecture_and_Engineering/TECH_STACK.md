# Technology Stack v1.2 (Frozen Tech Stack)

## 1. Backend Core & Frameworks
* **Runtime**: Java 17 LTS (OpenJDK 17)
* **Framework**: Spring Boot 3.3.2 (Modular Monolith)
* **Security & Auth**:
  - Spring Security 6.3.x
  - JJWT (Java JWT) `0.12.6` (`io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
  - TOTP: RFC 6238 compliant HMAC-SHA1 / HMAC-SHA256 implementation
  - Encryption: AES-GCM (128-bit tag, 12-byte IV) using Java Cryptography Architecture (JCA)
* **Persistence & ORM**:
  - Spring Data JPA (Hibernate 6.5.x)
  - Flyway Migration `10.15.x` (`flyway-core`, `flyway-mysql`)
  - MySQL Connector/J `8.3.0`
* **Observability & Diagnostics**:
  - Spring Boot Starter Actuator 3.3.2 (exposing `/actuator/health` only)

## 2. Databases & Storage
* **Relational Database**: MySQL 8.0.36 (`InnoDB`, `utf8mb4_unicode_ci`)
* **Caching & Session Layer**: Redis 7.2 Alpine (Append-Only File `AOF` enabled, protected by `requirepass`)
* **Object Storage & Cloud Messaging**:
  - Google Cloud Storage / Firebase Storage (private bucket `users/{userId}/images/{imageId}`)
  - Firebase Cloud Messaging (FCM)
  - Firebase Admin SDK `9.3.x`

## 3. Artificial Intelligence (Backend Only)
* **LLM Engine**: Google Gemini REST API (Adapter via Spring `RestClient`)
* **AI Configuration (Decision D3)**:
  - Model: `<SET_ME>` (TBD, provided by PO)
  - API Version: `<SET_ME>` (TBD, provided by PO)
  - Fail-Fast Mechanism: Throws `AI_NOT_CONFIGURED` if unconfigured.
* **Response Contract**: JSON Schema draft 2020-12 ([ai_response.schema.json](file:///d:/AIStudyMentor/docs/ai_response.schema.json))

## 4. Mobile Client (Android)
* **IDE & Toolchain**: Android Studio Iguana / Jellyfish, Gradle 8.x (Kotlin DSL)
* **Language & UI Toolkit**: Kotlin 2.0.x, Jetpack Compose, Material 3 (1.2.x), Navigation Compose (2.7.x)
* **Architecture**: MVVM, Kotlin Coroutines, StateFlow
* **Networking & Caching**: Retrofit 2.11.0, OkHttp 4.12.0, Room 2.6.x (offline SQLite cache), Coil 2.6.0 (image loading)
* **Hardware & Camera**: CameraX 1.3.x
* **Dependency Injection**: Dagger Hilt 2.51.x

## 5. Testing & Quality Assurance
* **Testing Framework**: JUnit 5 (Jupiter), Mockito, AssertJ
* **Integration Testing**: Testcontainers 1.19.8 (`testcontainers`, `mysql`, `junit-jupiter`)
* **Email Testing (Local Development)**: Mailpit (SMTP: `127.0.0.1:1025`, Web UI: `127.0.0.1:8025`)
