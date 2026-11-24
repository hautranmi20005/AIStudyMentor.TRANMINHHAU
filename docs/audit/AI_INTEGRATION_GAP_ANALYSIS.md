# AI Integration Gap Analysis

## Scope limitation

The requested `AI_Study_Mentor_Product_Completion_Blueprint.md` and its sections 6.3, 6.4, and 8.3 were not found. The exact comparison to those sections is therefore **blocked by missing source material**. This report compares implementation with the available [`AI_ORCHESTRATOR_SPEC.md`](../../Architecture_and_Engineering/AI_ORCHESTRATOR_SPEC.md), [`SRS.md`](../../Core/SRS.md), and code; it does not assume the missing Blueprint's contents.

## Existing implementation versus available spec

| Area | Available specification | Observed implementation | Gap/status |
|---|---|---|---|
| Provider adapter | Server-only Gemini adapter | `GeminiAdapterService` calls the Google Generative Language REST API with an API-key header; Android calls the backend only. | Adapter exists; no live Gemini call was performed in this audit. |
| Text request construction | Subject and question prompt | Adapter builds a system instruction and question text; response requests JSON MIME type and a response schema. | Present, but the normal unconfigured submit path does not surface `AI_NOT_CONFIGURED`; it persists without an answer. |
| Image input | Ownership validation, then image to Gemini | Adapter accepts bytes/MIME and can emit inline image data. Question submission uses the text-only overload; upload authorization only synthesizes a path and random token, attachments are empty, and no Firebase storage read/ownership validation is wired. | Image question path is a stub. |
| Schema | Strict response matching `docs/ai_response.schema.json` | Adapter creates a response schema in code, deserializes `GeminiStructuredExplanation`, and applies validation; invalid output is retried once. | Some runtime enforcement exists. No evidence that the checked-in JSON Schema file is loaded/validated directly, and no live provider result was checked. |
| Safety | Academic tutoring, validate content, ownership and inputs | Prompt asks for careful tutoring and not to infer personal attributes from images; input validation covers supported image MIME/size and request constraints. No Gemini `safetySettings` or independent safety/moderation stage was found. | Basic prompt/input safeguards only; full safety behavior is unverified and below the available pipeline's ownership-validation description. |
| Exact-match cache | Redis key `qa:v1:{model}:{promptVersion}:{hash}`, <200 ms hit, 7-day TTL | No question/answer Redis cache key or read/write path was found. `QuestionService` writes `cacheHit(false)` for generated answers. Other Redis uses and profile/dashboard cache annotations do not implement answer caching. | Not implemented; no latency test. |
| Usage logging | Input/output tokens, latency, cache-hit flag | `AiGenerationLog` has input/output token and latency/cache columns. The question path sets model, prompt version, total latency, and `cacheHit(false)`; no provider token usage is captured. | Persistence model exists; actual token usage logging is incomplete. |
| Error behavior | Fail fast with `AI_NOT_CONFIGURED`; reject malformed output; safe provider error | Adapter maps provider failures and invalid output to controlled errors and has a configuration exception. Alternative-explanation path validates configuration. Normal text submit conditionally avoids the adapter and returns `SUBMITTED` without an answer. | Error mapping partly exists; user-facing main flow violates fail-fast/no-success-shaped-fallback expectation. |
| Local fallback | Available spec says malformed provider output should be retried/rejected | `QuestionService.generateSmartLocalExplanation` fabricates heuristic answers and logs model `local-smart-solver`. A backend-only test with Gemini unset showed ordinary question submit returned no answer; the source includes the local fallback in `generateAiAnswer` but the submit method calls that only when configured. | Fake code exists; no evidence it was reached in tested public flow. It must not be used as a replacement for a real provider response. |
| Alternative explanations | Dedicated alternative explanation request | Backend endpoint and adapter flag exist. | Not exposed through Android `ApiService`; provider path not exercised. |
| Quiz generation | SRS requires AI-generated quizzes and secure server-side scoring | `QuizService.generateQuiz` calls `AiOrchestrator`, validates generated questions, persists the answer key server-side, and returns quiz DTOs without correct answers. The Android UI supports generation and pre-attempt subject edit/delete. | Unit, mocked-Gemini Testcontainers integration, and focused emulator tests pass. No live Gemini call or P19 quiz walkthrough was performed; live content quality remains unverified. |

## Runtime evidence

The backend was started on Java 25 with in-memory H2 and Gemini explicitly unset. HTTP registration returned 201, login succeeded, and text-question submission returned `SUBMITTED` with no answer. This proves only that the backend API can persist a local question in this configuration. It does not validate the Android UI, MySQL, Redis, Firebase, live Gemini, answer quality, cache latency, or token accounting.

## Referenced source locations

- Adapter, request/prompt construction, retry/error mapping, schema and validation: [`GeminiAdapterService.java`](../../backend/src/main/java/com/aistudymentor/ai/service/GeminiAdapterService.java)
- Question orchestration, empty attachments, local solver, false cache-hit logging and mapping: [`QuestionService.java`](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- AI telemetry entity: [`AiGenerationLog.java`](../../backend/src/main/java/com/aistudymentor/ai/entity/AiGenerationLog.java)
- Runtime Gemini defaults: [`application.yml`](../../backend/src/main/resources/application.yml); dev placeholders: [`application-dev.yml`](../../backend/src/main/resources/application-dev.yml)
- Checked-in response shape: [`ai_response.schema.json`](../ai_response.schema.json)
- Gemini and Android/API requirements: [`AI_ORCHESTRATOR_SPEC.md`](../../Architecture_and_Engineering/AI_ORCHESTRATOR_SPEC.md), [`SRS.md`](../../Core/SRS.md)

## Decisions/input needed

1. Supply the missing Blueprint and Technical SPEC v2 to complete the requested section-by-section comparison.
2. Confirm Gemini model/API version and provide local credentials through an untracked secret mechanism; do not use or disclose the tracked `.env` values.
3. Confirm approved development Firebase Storage behavior and ownership-validation mechanism. No approved Firebase Admin/FCM implementation decision was found in the current decision log.
4. Resolve the Java 17 baseline versus Java 25 project configuration with a recorded ADR before subsequent implementation work.
