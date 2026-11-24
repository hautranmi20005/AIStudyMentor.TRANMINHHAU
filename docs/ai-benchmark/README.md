# AI benchmark

`questions.json` contains 32 curated academic questions with deterministic answer-key facts/calculations and 10 adversarial/off-topic requests. The runner evaluates the structured answer endpoint and explicit refusal error codes.

Run against a locally running backend configured for Gemini. Obtain a short-lived access token through the normal authentication flow; do not put it in source control or command history:

```powershell
$env:AI_BENCHMARK_BEARER_TOKEN = "<short-lived access token>"
python .\docs\ai-benchmark\run.py --limit 5
python .\docs\ai-benchmark\run.py
```

By default, the runner uses `http://127.0.0.1:8080`, writes `REPORT.md`, and submits all 42 samples. `--base-url`, `--limit`, `--token`, and `--output` can override those defaults. Each non-refusal request consumes a Gemini call unless a per-user answer-cache hit occurs. Real runs can incur provider charges.

The curated answer keys are simple facts or calculations; they have not been independently reviewed by a subject-matter expert. Correctness is a substring check against the answer, summary, and step explanations, not expert evaluation. Adversarial pass criteria check only explicit API refusal codes. The local fake-Gemini HTTP cases run in `GeminiAdapterServiceTest` via MockWebServer; they are deterministic transport/schema tests, not benchmark-quality or model-quality evidence.

The harness currently measures end-to-end response latency and token counts returned by the backend. Provider token values may be absent on errors or cache hits. Keep the token and response content out of logs and reports.
