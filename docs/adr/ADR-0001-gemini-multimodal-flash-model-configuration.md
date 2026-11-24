# ADR-0001: Gemini Multimodal Flash Model Configuration

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

AI explanations require a multimodal provider for text and image inputs. Model identifiers and API versions change over time, so embedding them in application code makes upgrades harder and can silently pin the service to an unsupported model. The AI provider must remain on the backend; the Android application must not receive the provider key.

## 2. Decision Drivers & Alternatives Considered

- Keep model and API version configurable independently of source code.
- Fail safely when the provider is not configured; do not return fabricated answers or silently switch providers/models.
- Keep provider credentials out of Android, responses, and logs.
- Alternatives considered: hard-coded model defaults; a local “smart solver” fallback; provider selection in the Android client.

## 3. Decision Outcome & Justification

Configure the active Gemini model with `GEMINI_MODEL` and its API version with `GEMINI_API_VERSION`. The backend `AiProvider` adapter is the only provider integration point. Ask AI returns an explicit configuration/provider error rather than synthesizing an answer when Gemini is unavailable. Quiz generation may use a clearly logged static study-skills template when generation fails so the quiz workflow remains usable.

The application defaults to the currently supported `gemini-3.8-flash` model, while deployments can override it through `GEMINI_MODEL`. Gemini response failures are logged with the provider HTTP status and a sanitized, bounded error message. Student-facing quiz responses continue to omit correct answers; answer keys and explanations are available through the existing edit and grading responses.

## 4. Consequences

- **Positive**: Model/API changes can be made through deployment configuration, and provider credentials remain server-side.
- **Negative**: Each runtime environment must supply compatible model and API-version values. Misconfiguration must be surfaced rather than hidden by a fallback.
- The default model must be kept aligned with Google's supported model catalog. Quiz fallback behavior must not be used for Ask AI explanations or bypass content-safety rejections.

## 5. References & Code Artifacts

- Configuration: `GEMINI_MODEL`, `GEMINI_API_VERSION`, `GEMINI_API_KEY`
- [application.yml](../../backend/src/main/resources/application.yml)
- [GeminiAdapterService.java](../../backend/src/main/java/com/aistudymentor/ai/service/GeminiAdapterService.java)
- [QuestionService.java](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- [AI integration gap analysis](../audit/AI_INTEGRATION_GAP_ANALYSIS.md)
