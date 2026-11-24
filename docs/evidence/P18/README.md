# P18 Product Polish Evidence

## Verification summary

All commands used Temurin JDK 17.0.21. Android Gradle commands used `--no-daemon` and `-Pkotlin.compiler.execution.strategy=in-process`.

| Check | Result | Evidence |
|---|---|---|
| Android JVM unit tests | PASS; 18 passed, 0 failed/errors/skipped | [Final Android validation log](./android-final-validation.log) |
| Android connected tests | PASS; 18 passed, 0 failed/errors/skipped on API 37 emulator | [Final Android validation log](./android-final-validation.log) |
| Android lint | PASS; 0 `HardcodedText` findings (42 total lint findings) | [Final Android validation log](./android-final-validation.log), `android/app/build/reports/lint-results-debug.xml` |
| Backend quota integration class | PASS; 6 passed, 0 failed/errors/skipped | [Backend quota integration log](../P18-backend-quota-integration.log) |
| English/Vietnamese resources | PASS; 188 keys per locale, no missing keys | Verified from the Android resource XML |

The backend run targeted `BackendEndToEndIntegrationTest`; it was not a full Maven test suite. Its XML report is generated at `backend/target/surefire-reports/TEST-com.aistudymentor.BackendEndToEndIntegrationTest.xml`.

## UI screenshots

Screenshot tests use fake repositories and synthetic content. The Ask screen therefore shows the expected quota-unavailable state; no live account information is present.

- Default phone, light/dark: [Ask light](./screenshots/p18-ask-light.png), [Ask dark](./screenshots/p18-ask-dark.png), [Quiz light](./screenshots/p18-quiz-light.png), [Quiz dark](./screenshots/p18-quiz-dark.png).
- 200% font scale: [Ask light](./font-200/screenshots/p18-ask-light.png), [Ask dark](./font-200/screenshots/p18-ask-dark.png), [Quiz light](./font-200/screenshots/p18-quiz-light.png), [Quiz dark](./font-200/screenshots/p18-quiz-dark.png).
- Compact 480x800: [Ask](./small-screen/screenshots/p18-ask-light.png), [Quiz](./small-screen/screenshots/p18-quiz-light.png).
- Tablet-sized 1280x800: [Ask](./tablet/screenshots/p18-ask-light.png), [Quiz](./tablet/screenshots/p18-quiz-light.png).

## Accessibility and handoff

See [ACCESSIBILITY_CHECKLIST.md](./ACCESSIBILITY_CHECKLIST.md) for contrast calculations, verified checks, and explicit accessibility gaps. The project-level handoff is in [PROGRESS_LOG.md](../../PROGRESS_LOG.md).
