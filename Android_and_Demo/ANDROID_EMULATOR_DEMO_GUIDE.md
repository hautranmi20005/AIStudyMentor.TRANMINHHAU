# Android Emulator Demo Guide v1.1

## 1. Setup
- Backend running locally on port 8080 (Docker Compose for MySQL/Redis).
- Set Emulator Base URL to `http://10.0.2.2:8080/api/v1` (Debug config only).

## 2. Demo Script
1. Register user (DOB > 13).
2. Setup MFA.
3. Ask Math question (Cache Miss).
4. Ask same Math question (Cache Hit).
5. Generate Quiz, complete it, and view score.
6. View Progress Dashboard.
7. Register U13 user -> show blocked Consent screen.
