# AI benchmark report

- Run time (UTC): 2026-11-17T12:16:52Z
- Backend: `http://127.0.0.1:8765`
- Samples run: 42 (answer: 32, adversarial/refusal: 10)
- Answer correctness: 32/32 (100.0%)
- Schema-valid answers: 32/32 (100.0%)
- Correct refusals: 10/10 (100.0%)
- p50 latency: 13 ms
- p95 latency: 25 ms
- Reported tokens: unavailable
- Failed/errored requests: 10

## Limitations

- Correctness is a deterministic expected-answer substring check, not expert grading.
- The backend is responsible for Gemini provider selection, prompts, quotas, and output validation.
- Real Gemini runs consume provider tokens; use `--limit` to control sample size.
- Adversarial refusal correctness is counted only for explicit `AI_OUT_OF_SCOPE` or `AI_BLOCKED_CONTENT` errors.
- This runner requires an authenticated backend token and does not print tokens or response bodies.

## Team Baseline Targets

| Metric | Target | Stub-run result |
|---|---:|---|
| JSON Schema Validity | 100% | 32/32 structured answer responses passed the runner's required-field checks (100%); this is not an independent JSON Schema conformance suite |
| Scope Refusal Accuracy | >= 90% | 10/10 expected refusals (100%) |
| Zero Prompt/Secret Leakage | 100% | Not measured by this harness |

## Known failure categories

- The 10 adversarial samples intentionally return HTTP 422 (`AI_OUT_OF_SCOPE`). The benchmark runner counts those expected refusals under “Failed/errored requests”; they are not failed refusal checks.
- This deterministic stub cannot expose model-quality, prompt-injection, malformed-model-output, or real Gemini leakage failures. Those categories remain unverified.
