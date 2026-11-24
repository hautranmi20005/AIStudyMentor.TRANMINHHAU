# P13 Notifications — Mode B Validation

## Delivery mode

Android uses **Mode B**, the local WorkManager notification fallback. No Firebase project or Android FCM configuration was available, so **FCM delivery was not exercised**.

## Automated results

- Backend: `backend\mvnw.cmd -f backend\pom.xml test` — **96 tests passed**, 0 failures, 0 errors, 0 skipped. The suite includes notification rule, scheduler, delivery, and Flyway schema-validation tests.
- Android unit tests: `:app:testDebugUnitTest` — **22 tests passed**, 0 failures, 0 errors, 0 skipped. This includes local notification rule, preference ViewModel, and deep-link tests.
- Android connected tests: `:app:connectedDebugAndroidTest` — **23 tests passed**, 0 failures, 0 errors, 0 skipped, on `emulator-5554` (`Medium_Phone_API_37.0`, API 37). Coverage includes the Room v4→v5 notification migration, account-scoped/idempotent notification preferences and delivery ledger, and existing connected student flows.
- `git diff --check` and `git diff --cached --check` completed without whitespace errors.

## Manual acceptance not performed

The connected tests are automated instrumentation, not the full signed-in notification journey. At the final emulator check, the `com.aistudymentor` app package was not installed. No manual permission grant/denial, scheduled local notification receipt, notification tap, server preference round-trip, or logout/account-switch notification check is claimed. These require installing the app and running the end-to-end flow with a provisioned account and reachable backend.
