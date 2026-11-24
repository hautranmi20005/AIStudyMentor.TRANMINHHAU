# P04 Backend Foundation Implementation Notes

## API coverage

The authoritative endpoint inventory and REST Client request collection are [API_SPEC.md](./API_SPEC.md) and [AIStudyMentor.http](./requests/AIStudyMentor.http). There are 26 application controller routes plus management health and metrics requests. The current runtime does not have notification or privacy/account-deletion controllers.

## Package boundary review

The backend is organized into `auth`, `profile`, `question`, `ai`, `quiz`, and `dashboard` packages, with shared `common` and `config` packages. The following cross-module dependencies are present:

| Caller | Direct dependency | Assessment |
|---|---|---|
| `auth.service.AuthService` | Profile entity/repository | Registration creates a student profile as part of the account flow. This is a workflow-level module dependency, not a local import cleanup. |
| `question.service.QuestionService` | Profile entities/repository and AI entities/repository/service | Question orchestration reaches directly into persistence and provider implementation details across module boundaries. |
| `quiz.service.QuizService` and quiz entities | Profile entity/repository | Quiz ownership and persistence are coupled to profile persistence types. |
| `dashboard.service.DashboardService` | Profile, question, and quiz entities/repositories | Dashboard aggregates persistence from three modules directly. |
| All feature modules | `common` | Shared DTOs, exceptions, utilities, and base entity are expected cross-cutting dependencies. |

No separate `notification` or `privacy` module/package is present. `common` and `config` are cross-cutting infrastructure, not substitute feature modules. These structural changes are not trivial; this task reports them without moving modules or changing persistence behavior.

## Operational and security notes

- Management endpoints bind to `127.0.0.1:8081`, separate from the API port. Readiness checks MySQL and Redis; the Gemini component reports configuration only and makes no provider request.
- No OpenAPI dependency was added. A checked-in `.http` collection covers the controller routes without adding a dependency or service.
- The sensitive-log filter blocks recognizable credential/token/OTP, prompt-label, and image-payload patterns. Application logs were also audited to avoid logging request bodies or exception messages. This is defense in depth; new logging must continue to exclude raw request data.
- Health, metrics, and error responses do not include provider secrets or stack traces.

## Verification outcomes

- `mvnw.cmd -B clean verify` passed all 49 tests with zero failures, errors, or skips.
- The final targeted P04 run passed 12 tests, including a Testcontainers-backed invalid registration request asserting the standard error shape and correlation ID.
- The Testcontainers-backed profile flow exposed a cache initialization defect: wrapping `RedisCacheManager` prevented Spring from invoking its lifecycle callback, so named JSON cache configurations were not loaded and profile DTOs fell back to JDK serialization. `MeteredCacheManager` now initializes its delegate; the profile flow and cache-manager regression tests pass.
- The local MySQL, Redis, and Mailpit containers were healthy on alternate host ports after the default Redis port was found occupied by pre-existing `studymentor_redis`. The dev backend did not reach a running state: after the PowerShell Maven argument and JDK selection issues were corrected, Spring context initialization still failed before the API became available. No live actuator response or live bad-request response was obtained; the latter is covered by the passing HTTP integration test instead.
- `scripts/dev-backend.ps1` now quotes the Spring profile property so PowerShell passes it as one Maven argument. Startup also requires selecting the project JDK (JDK 25 in this environment).
