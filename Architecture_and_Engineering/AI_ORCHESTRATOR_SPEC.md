# AI Orchestrator Specification (Gemini) v1.2

## 1. Orchestration Pipeline

```
Student Prompt (Text / Image Path)
   │
   ▼
[1. Safety & Ownership Validation]
   ├── Verify image ownership in Firebase Storage (users/{userId}/images/...)
   └── Validate input size & prompt length
   │
   ▼
[2. Exact-Match Cache Layer]
   ├── Key: qa:v1:{model}:{promptVersion}:{sha256(subject + promptText + imageHash)}
   └── Cache Hit -> Return structured JSON in <200ms (NFR-02)
   │
   ▼ (Cache Miss)
[3. Gemini Adapter Invocations]
   ├── Validate AI credentials (fails fast with AI_NOT_CONFIGURED if unconfigured)
   ├── System Prompt: Demands strict JSON matching docs/ai_response.schema.json
   └── Parse response JSON & execute schema validation
   │
   ▼
[4. Telemetry & Persistence]
   ├── Store log in ai_generation_logs (input/output tokens, latency, cache_hit)
   ├── Store in answers table (raw_content + structured_json)
   └── Cache answer in Redis with 7-day TTL
```

---

## 2. Structured Output & Validation Contract

The AI response MUST strictly conform to [`docs/ai_response.schema.json`](file:///d:/AIStudyMentor/docs/ai_response.schema.json) and [`GeminiStructuredExplanation`](file:///d:/AIStudyMentor/backend/src/main/java/com/aistudymentor/ai/dto/GeminiStructuredExplanation.java):
* `summary`: String (min length 10)
* `steps`: Array of objects (`stepNumber`, `title`, `explanation`, optional `formula` in KaTeX/LaTeX)
* `finalAnswer`: String (min length 1)
* `keyTakeaways`: Array of Strings (min items 1)
* `alternativeExplanation`: String (optional, populated for FR-05)

*Validation Enforcement*: Any LLM response that violates the schema is immediately rejected and discarded; fallback retry is triggered before returning an error to the user.

---

## 3. Configuration & Pending PO Decisions (D3)

* **Gemini Model ID**: Configured as placeholder `<SET_ME>`.
* **API Version**: Configured as placeholder `<SET_ME>`.
* **Runtime Behavior**: Application boots normally; any AI call when model is `<SET_ME>` fails fast with `AI_NOT_CONFIGURED`.
