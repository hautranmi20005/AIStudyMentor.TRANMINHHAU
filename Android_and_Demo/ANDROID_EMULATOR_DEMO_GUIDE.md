# Android Emulator Demo Guide v1.2

This guide outlines local setup and testing procedures on Android Emulator (API Level 34) connecting to the Spring Boot backend on the host machine.

---

## 1. Network & Environment Setup

### 1.1 Local Backend Execution
Start all required services via Docker Compose inside `/backend`:
```bash
docker compose up -d
```
Verify all 3 services are healthy:
- MySQL: `127.0.0.1:3306`
- Redis: `127.0.0.1:6379`
- Mailpit: `127.0.0.1:8025` (Web UI for guardian verification links)

Run the Spring Boot application using the `dev` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 1.2 Android Emulator Network Address
The Android Emulator uses the special alias `10.0.2.2` to access `127.0.0.1` on the development host.
* **Debug Base URL**: `http://10.0.2.2:8080/api/v1`
* **Cleartext Traffic Configuration**: In `android/app/src/debug/res/xml/network_security_config.xml`, cleartext HTTP traffic is explicitly whitelisted **only** for debug builds:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <network-security-config>
      <domain-config cleartextTrafficPermitted="true">
          <domain includeSubdomains="true">10.0.2.2</domain>
      </domain-config>
  </network-security-config>
  ```
  *(Production builds strictly mandate HTTPS).*

---

## 2. Step-by-Step Demo Script

### Step 1: Register Standard Student (Age >= 13)
* Open the app, navigate to **Register**.
* Input Email: `student14@example.com`, Password: `Password123!`.
* Select DoB resulting in age >= 13 (e.g., `2010-01-01`).
* Notice: `guardianEmail` field is hidden/optional.
* Tap **Register**. Account is created with `account_status == ACTIVE`.

### Step 2: Enroll & Verify TOTP MFA (Decision D1)
* From Profile/Security settings, tap **Setup Two-Factor Authentication**.
* Client calls `POST /api/v1/auth/mfa/enroll`.
* Screen displays QR Code and manual secret string (e.g., `JBSWY3DPEHPK3PXP`).
* In Google Authenticator / 2FAS (or CLI tool `oathtool --totp -b JBSWY3DPEHPK3PXP`), obtain the 6-digit code.
* Enter code on `MfaEnrollmentScreen` and tap **Activate**. Backend validates and sets `is_verified = true`.
* Log out and log back in: Observe that `POST /auth/login` returns `mfaRequired: true` and `mfaSessionToken`.
* Enter current TOTP code on `MfaVerificationScreen` (no resend button exists). Full Access/Refresh tokens are issued.

### Step 3: Ask Text Question (Cache Miss & Hit)
* Navigate to **Ask AI**, choose subject `Mathematics`.
* Enter text query: *"Solve for x: 2x + 6 = 14"*.
* Tap **Solve Problem**. The first execution hits Gemini (Cache Miss), logs latency, and renders step-by-step Material 3 cards.
* Submit the identical question again: The response arrives in <50ms from Redis exact-match cache (Cache Hit).
* Tap the Bookmark icon: Calls `POST /questions/{id}/bookmark`. Navigate to **Bookmarks** tab to see the saved question.

### Step 4: Photo Question via CameraX & Firebase Storage
* On **Ask AI**, tap the Camera icon.
* Capture a photo of a math problem using CameraX viewfinder.
* App requests upload authorization via `POST /uploads/authorize`.
* App uploads photo to private Firebase Storage bucket `users/{userId}/images/{imageId}`.
* App calls `POST /questions` with `imageStoragePath`.
* Backend verifies ownership and invokes Gemini multimodal model. Step-by-step solution renders on screen.

### Step 5: Practice Quiz Flow & Server-Side Scoring (Decision D8)
* Navigate to **Quizzes** tab, tap **Generate Quiz** (Subject: Mathematics, 3 questions).
* Backend creates quiz and returns questions without answers.
* Complete the quiz (1 question per screen with progress bar).
* On final screen, tap **Submit Quiz**.
* Backend evaluates responses server-side and returns the scored attempt.
* View results with correct answers and explanations on `QuizResultScreen`.

### Step 6: View Progress Dashboard (FR-08)
* Navigate to **Dashboard** tab.
* Observe aggregated statistics: Total questions solved, total quizzes completed, and accuracy rate breakdown by subject.

### Step 7: Register Under-13 User & Guardian Consent Blocker (Decision D2, D9)
* Log out and register a new student: `child@example.com`.
* Select DoB indicating age < 13 (e.g., `2016-05-10`).
* Notice: `guardianEmail` field becomes required. Enter parent's email: `parent@example.com`.
* Tap **Register**. Account is created in `PENDING_CONSENT` state.
* App navigates to `GuardianConsentScreen` blocking access to study features.
* Try navigating to `AskAiScreen` or tapping Bottom Nav: System blocks with HTTP 403 `GUARDIAN_CONSENT_REQUIRED`.
* Open Mailpit Web UI at `http://localhost:8025`: Find the verification email containing the 72-hour signed verification link.
* Click the link (`GET /guardian-consent/verify?token=...`). The account transitions to `ACTIVE`.
* On Emulator, tap **Check Status**: App immediately unblocks without requiring re-login.
