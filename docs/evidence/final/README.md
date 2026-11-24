# P19 Evidence Index

This folder contains P19 evidence only. A passing build/test result is not presented as proof of the complete demo.

| Evidence | What it proves |
|---|---|
| [Acceptance checklist](./ACCEPTANCE_CHECKLIST.md) | P19 gates, feature statuses, and release decision |
| [Backend startup note](./backend-startup.md) | Sanitized diagnosis, local config correction, and successful health-checked bootstrap |
| [Ask AI connection diagnosis](./ask-ai-connection.md) | Backend log analysis, timeout handling changes, and focused test results |
| [Flyway history](./flyway-history.txt) | Observed successful local migration rows V1–V16 |
| [Debug app launch screenshot](./screenshots/debug-app-launch-only.png) | The archived APK installed and reached the sign-in screen on `emulator-5554`; no authentication or backend journey is shown |
| [Archived debug APK](./artifacts/AIStudyMentor-debug-p19.apk) | Exact APK built during P19 validation; SHA-256 is recorded in the checklist |
| [Android result XML](./android/) | Unit-test and connected-test XML, plus lint XML, copied from the P19 Gradle outputs |
| [Partial Maven results](./backend/MAVEN-VERIFY-STATUS.md) | 29 completed tests from 9 fresh Surefire XML files; full `verify` was interrupted during the integration suite and is not reported as passing |

No P19 core-flow screenshots or screen recordings were captured. Existing P08/P18 evidence is historical and is not included as P19 journey evidence.
