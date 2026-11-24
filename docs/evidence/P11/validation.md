# P11 Practice Quiz Validation

## Automated results

- Backend: `backend\mvnw.cmd test` — **86 tests passed; 0 failures, 0 errors, 0 skipped**. Includes the Flyway schema-validation integration test, authenticated quiz generation/attempt/replay/ownership integration coverage, deterministic answer normalization tests, and AI-generation rate-limit tests.
- Android unit tests: `:app:testDebugUnitTest` — **15 tests passed; 0 failures**.
- Android instrumented tests: `:app:connectedDebugAndroidTest` on `emulator-5554` — **15 tests passed; 0 failures**. Includes the fake-repository Compose quiz generation, unanswered confirmation, grading result, and Ask AI flow.
- Android instrumented source compilation: `:app:compileDebugAndroidTestKotlin` — passed.

The backend integration test verifies that pre-submit quiz DTOs omit answer/explanation fields, client-supplied correctness is ignored, the server grades responses, resumed attempts are reused, replay returns `409`, and another user's review returns `404`.

## Not verified in this run

- No live Gemini quiz was generated from the emulator; the Compose flow uses a fake repository, and backend integration uses the test provider.
- No live database score query, mid-quiz force-stop/relaunch, or captured pre-submit network trace was performed.
- No screenshot is included; the emulator evidence is the successful instrumented-test result. These live acceptance checks remain outstanding and are not inferred from build/test success.
