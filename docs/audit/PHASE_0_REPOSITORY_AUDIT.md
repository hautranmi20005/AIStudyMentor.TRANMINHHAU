# Phase 0 Repository Audit

Audit date: 2026-11-10  
Scope: read/build/run audit only. No application source or configuration was changed for this audit. Four audit reports are being added under `docs/audit/`.

## 1. Repository snapshot

Snapshot was recorded before writing these reports.

| Item | Result |
|---|---|
| HEAD | `b2debdc2977a5445a4a174537e7180e7105ec1db` |
| Branch | `main` |
| Worktree | Dirty before audit; still dirty after builds |
| Status entries before report creation | 549: 430 modified, 118 deleted, 1 untracked |
| Existing unrelated/local changes before this audit | `.github/workflows/ci-cd.yml`, `Architecture_and_Engineering/TECH_STACK.md`, `android/.gradle/`, `backend/.env`, `backend/Dockerfile`, `backend/pom.xml`, `backend/src/main/resources/application.yml`; build/test tasks also modified tracked generated files |
| Tracked file total | 3,793 |
| Files added by this audit | Four Markdown reports; not included in the snapshot counts above |

### Tracked files by top-level folder

| Folder | Count |
|---|---:|
| Repository root | 5 |
| `.github` | 5 |
| `.idea` | 11 |
| `.vs` | 12 |
| `.vscode` | 1 |
| `android` | 3,379 |
| `Android_and_Demo` | 2 |
| `Antigravity_Agent_Documentation` | 11 |
| `Architecture_and_Engineering` | 8 |
| `backend` | 345 |
| `Core` | 7 |
| `docs` | 2 |
| `Reports` | 5 |
| **Total** | **3,793** |

| Tracked generated/IDE path segment | Count |
|---|---:|
| `build/` | 3,280 |
| `target/` | 206 |
| `.gradle/` | 27 |
| `.idea/` | 24 |
| `.vs/` | 12 |
| **Total** | **3,549** |

The ignore rules mention environment files and build outputs in [`.gitignore`](../../.gitignore), but those files are already tracked; ignore rules do not untrack them.

## 2. Source-document availability

Available requirements/spec sources reviewed:

- [`Core/SRS.md`](../../Core/SRS.md)
- [`Core/MVP_SCOPE.md`](../../Core/MVP_SCOPE.md)
- [`Core/DECISION_LOG.md`](../../Core/DECISION_LOG.md)
- [`Architecture_and_Engineering/API_SPEC.md`](../../Architecture_and_Engineering/API_SPEC.md)
- [`Architecture_and_Engineering/DATABASE_DESIGN.md`](../../Architecture_and_Engineering/DATABASE_DESIGN.md)
- [`Architecture_and_Engineering/AI_ORCHESTRATOR_SPEC.md`](../../Architecture_and_Engineering/AI_ORCHESTRATOR_SPEC.md)

The requested `AI_Study_Mentor_Product_Completion_Blueprint.md` and `AI Study Mentor Technical SPEC v2` are not present in the workspace. Therefore, this audit does not infer Blueprint sections 6.3, 6.4, or 8.3; see [AI integration gap analysis](./AI_INTEGRATION_GAP_ANALYSIS.md).

The user-provided baseline says Java 17 and disallows changing it without an approved ADR. The current backend POM, Dockerfile, CI and edited technology-stack document use Java 25, while the decision log has no Java-version ADR. This unresolved governance mismatch is recorded as a blocker, not changed here.

## 3. Build and test results

Commands were run from their project folders. The backend wrapper exists at [`backend/mvnw.cmd`](../../backend/mvnw.cmd); the Android wrapper exists at [`android/gradlew.bat`](../../android/gradlew.bat).

### Backend

1. Exact requested verification under the shell's default Java 17:

   ```powershell
   .\mvnw.cmd -B verify
   ```

   **Failed, exit 1.** Surefire could not load test classes compiled for Java 25 (class-file version 69); the Java 17 test runtime supports up to version 61. This is a JDK/worktree bytecode mismatch, not a test assertion failure. The command did not complete the test suite.

2. Same wrapper verification with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot`:

   ```powershell
   .\mvnw.cmd -B verify
   ```

   **Succeeded, exit 0** (`BUILD SUCCESS`). Result: 31 tests, 0 failures, 0 errors, 4 skipped. All four skipped cases are in `BackendEndToEndIntegrationTest`; its class is annotated `@Testcontainers(disabledWithoutDocker = true)`, and the Testcontainers log reported no available Docker environment. The H2-backed Flyway context test ran, rather than being skipped.

### Android

Exact requested tasks:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The requested tasks `:app:assembleDebug` and `:app:lintDebug` completed successfully, but the overall command exited 1 during Gradle shutdown because it could not release its build-output-cleanup and execution-history locks. Thus the command as a whole is **failed**, despite the tasks completing. Gradle also reported deprecated APIs/features.

`:app:testDebugUnitTest` was `NO-SOURCE`; no Android unit test source files were discovered, so zero Android unit tests ran. Android instrumentation tests were not run; no emulator or `adb` executable was available.

## 4. Local run and text-question trace

The requested Docker MySQL/Redis/Mailpit stack could not start: `docker info` could not connect to the Docker engine. No `adb` executable or running emulator was found. Consequently, the Android UI-to-API path and MySQL/Redis/Mailpit integration were not testable.

To gather backend-only evidence, the backend was started without Docker, with an in-memory H2 database and Gemini explicitly unset for the process:

```powershell
.\mvnw.cmd -B -DskipTests spring-boot:run
```

Startup evidence in the process log: H2 connected, Tomcat started on port 8080, and `BackendApplication` started. `/actuator/health` returned HTTP 401 because management health is protected and no management credentials were supplied.

Using local HTTP requests against this H2-only process:

| Flow step | Observed result |
|---|---|
| `POST /api/v1/auth/register` | HTTP 201 |
| `POST /api/v1/auth/login` | Succeeded; returned an access token; `mfaRequired=false` for the newly created unverified factor |
| `POST /api/v1/questions` with a text question | Returned a question in `SUBMITTED` status with no answer |

The flow breaks at **answer generation**: with Gemini unset, the submit path persists the question but returns no answer. This did not exercise the Android screens, MySQL, Redis, Mailpit, Firebase, or a live Gemini provider. No screenshot was produced. The temporary backend process was stopped after the probes.

## 5. Secret scan

`gitleaks` and `trufflehog` were not installed. A fallback scan checked working-tree text and all six reachable Git revisions for common Google/AWS/GitHub/private-key/JWT token formats and inspected environment-setting names without emitting values.

Findings (values deliberately withheld):

- `backend/.env` is tracked and contains non-empty settings for database, Redis, JWT, MFA-encryption, and Gemini credentials.
- Credential-setting assignments also occur in the repository history for `backend/.env`.
- No common provider-token/private-key/JWT formats were matched by the fallback pattern scan. This does **not** prove that the values in `.env` are invalid or harmless; they were not checked with provider services.
- A broader credential-keyword heuristic flagged `Token` references in `android/app/build/outputs/mapping/release/seeds.txt`; the redacted matches were identifier-shaped and are treated as generated-code false positives, not verified credentials. No matched values were printed.

Treat the tracked environment credentials as potentially exposed. Rotation/revocation and an authorized history-remediation decision are required before relying on them. No secret values are included in these reports.

## 6. Hypothesis verification

`CONFIRMED` means the implementation evidence supports the hypothesis; `REFUTED` means it contradicts it; `PARTIAL` means part of the wording is true but needs qualification.

| ID | Result | Evidence and conclusion |
|---|---|---|
| H1 Redis not used as answer cache | CONFIRMED | Redis is used by auth/account-status code, but question generation always logs `cacheHit(false)` and no answer-cache read/write exists: [`QuestionService.java:204-205`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L204), [`AuthService.java:63`](../../backend/src/main/java/com/aistudymentor/auth/service/AuthService.java#L63), [`AccountStatusService.java:24`](../../backend/src/main/java/com/aistudymentor/auth/service/AccountStatusService.java#L24). Profile/dashboard cache annotations are not answer caching: [`ProfileService.java:30`](../../backend/src/main/java/com/aistudymentor/profile/service/ProfileService.java#L30), [`DashboardService.java:36`](../../backend/src/main/java/com/aistudymentor/dashboard/service/DashboardService.java#L36). |
| H2 image flow is a stub | CONFIRMED | Authorization returns a generated path/token but has no Firebase authorization/upload implementation: [`QuestionService.java:54-60`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L54). Submission initializes an empty attachment list: [`QuestionService.java:69`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L69). The real adapter supports image bytes, but the question service invokes its text overload: [`GeminiAdapterService.java:69-70`](../../backend/src/main/java/com/aistudymentor/ai/service/GeminiAdapterService.java#L69). |
| H3 fake local “smart solver” fallback exists | CONFIRMED | Local solver is selected in `generateAiAnswer` when unconfigured and returns hard-coded heuristic answers: [`QuestionService.java:182-186`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L182), [`QuestionService.java:218-257`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L218). The public text-submit branch skips generation entirely when Gemini is unconfigured; the local HTTP probe returned no answer. |
| H4 no firebase-admin / FCM | CONFIRMED | No Firebase Admin or Firebase Messaging dependency/service was found. Android dependencies include Analytics/Crashlytics, not messaging: [`android/app/build.gradle.kts:127-128`](../../android/app/build.gradle.kts#L127). Firebase telemetry is not FCM delivery. |
| H5 Quizzes & Profile screens are placeholders | CONFIRMED | Quiz button has an empty action and a hard-coded Calculus label: [`QuizzesScreen.kt:31`](../../android/app/src/main/java/com/aistudymentor/ui/quizzes/QuizzesScreen.kt#L31). Profile displays fixed example email/grade: [`ProfileScreen.kt:27-28`](../../android/app/src/main/java/com/aistudymentor/ui/profile/ProfileScreen.kt#L27). Backend quiz generation is also hard-coded (Option B): [`QuizService.java:59-85`](../../backend/src/main/java/com/aistudymentor/quiz/service/QuizService.java#L59). |
| H6 Android ApiService lacks refresh/profile/quiz/search/report/upload/FCM | PARTIAL | It has a quiz-attempt-history read but lacks refresh, profile, quiz-create/start/submit, search/report, upload authorization/storage, and FCM calls: [`ApiService.kt:136-187`](../../android/app/src/main/java/com/aistudymentor/data/remote/ApiService.kt#L136). Backend profile and quiz endpoints do exist: [`ProfileController.java:17-37`](../../backend/src/main/java/com/aistudymentor/profile/controller/ProfileController.java#L17), [`QuizController.java:31-82`](../../backend/src/main/java/com/aistudymentor/quiz/controller/QuizController.java#L31). |
| H7 no MFA / forgot-password screens | PARTIAL | A login TOTP challenge is present: [`LoginScreen.kt:63-101`](../../android/app/src/main/java/com/aistudymentor/ui/auth/LoginScreen.kt#L63). No enrollment/activation UI or forgot-password screen/endpoint was found. MFA enrollment/activation is specified in the API document, but absent from `AuthController`; it exposes verify, refresh, etc.: [`AuthController.java:53-70`](../../backend/src/main/java/com/aistudymentor/auth/controller/AuthController.java#L53). Forgot-password is not specified in the available SRS. |
| H8 no CameraX/Coil/Hilt | CONFIRMED | No CameraX, Coil, or Hilt dependency or source usage was found in the Android build/source. Room and Compose are present: [`android/app/build.gradle.kts:123-125`](../../android/app/build.gradle.kts#L123). |
| H9 N+1 query in listQuestions | CONFIRMED | The page query is followed by per-row bookmark existence lookup inside `.map`: [`QuestionService.java:97-98`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java#L97). |
| H10 prod compose PostgreSQL while dev/CI use MySQL | CONFIRMED | Dev uses MySQL [`application-dev.yml:9`](../../backend/src/main/resources/application-dev.yml#L9), CI uses MySQL [`ci-cd.yml:23-24`](../../.github/workflows/ci-cd.yml#L23), and production compose/profile use PostgreSQL [`docker-compose.prod.yml:2-3,63`](../../backend/docker-compose.prod.yml#L2), [`application-prod.yml`](../../backend/src/main/resources/application-prod.yml). |
| H11 secrets/build outputs tracked in git | CONFIRMED | `backend/.env` is tracked. There are 3,549 tracked files under build/target/.gradle/.idea/.vs; `.gitignore` does not untrack existing files. |
| H12 E2E tests skipped | CONFIRMED | Four E2E tests were skipped; the class opts out without Docker: [`BackendEndToEndIntegrationTest.java:60`](../../backend/src/test/java/com/aistudymentor/BackendEndToEndIntegrationTest.java#L60). |
| H13 no search/report/delete-account | CONFIRMED | No such routes were found in backend controllers or Android API; the question controller's delete route is only bookmark removal: [`QuestionController.java:84-88`](../../backend/src/main/java/com/aistudymentor/question/controller/QuestionController.java#L84). These features are not present in the available SRS. |
| H14 cleartext network config in main source set | REFUTED | Main network config disallows cleartext: [`network_security_config.xml:3`](../../android/app/src/main/res/xml/network_security_config.xml#L3). Debug explicitly permits HTTP to emulator `10.0.2.2` and localhost: [`network_security_config_debug.xml:3-7`](../../android/app/src/debug/res/xml/network_security_config_debug.xml#L3). |
| H15 GEMINI_MODEL/API_VERSION unset | PARTIAL | Main config has defaults `gemini-2.5-flash` and `v1beta`; dev profile has `<SET_ME>` placeholders: [`application.yml:75-77`](../../backend/src/main/resources/application.yml#L75), [`application-dev.yml:26-28`](../../backend/src/main/resources/application-dev.yml#L26). The local H2 probe unset the key, resulting in a saved question without an answer. |

## 7. Additional findings

1. **Runtime approval is unresolved.** Backend requires Java 25, while the user-specified frozen baseline is Java 17 and there is no Java-version ADR. The default-JDK verification failed on pre-existing Java 25 class files; Java 25 verification passed.
2. **The quiz implementation is synthetic.** The server creates generic `Practice Question #n` entries with the same correct answer, Option B; it does not call Gemini.
3. **Normal question submission does not enforce the documented fail-fast error.** It skips answer generation when unconfigured; the observed API response was `SUBMITTED` with no answer. The exception exists and is used on the alternative-explanation path.
4. **Database setup differs across environments.** The default application datasource is in-memory H2, dev/CI use MySQL, while prod uses PostgreSQL. The database design document identifies MySQL as the physical schema.
5. **Current Android build has no unit-test sources**, despite test dependencies. Compilation/lint do not verify UI behavior.
6. **Important project docs are missing or drifted.** The requested Blueprint and Technical SPEC v2 were not found; the exact approval and adapter decisions referenced as ADD-03/ADD-04 were not found in the current decision log.

## 8. Status terminology

Requirement-level statuses and gaps are in [`REQUIREMENTS_TRACEABILITY_MATRIX.md`](./REQUIREMENTS_TRACEABILITY_MATRIX.md). AI details and the source-document limitation are in [`AI_INTEGRATION_GAP_ANALYSIS.md`](./AI_INTEGRATION_GAP_ANALYSIS.md). The dependency-ordered backlog is in [`IMPLEMENTATION_READINESS_REPORT.md`](./IMPLEMENTATION_READINESS_REPORT.md).
