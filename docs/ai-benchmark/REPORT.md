# AI benchmark report

- Status: **Not run against Gemini in this workspace.**
- Reason: no live Gemini API request was performed; there are no measured correctness, schema, refusal, latency, or token results to report.
- Fake-provider coverage: MockWebServer tests cover successful structured output, 429/5xx retry behavior, timeout, malformed JSON repair, blocked content, auth error handling, and image payload transmission. These tests are not benchmark measurements.
- Limitations: dataset expected answers are simple curated facts/calculations and have not received independent subject-matter review; the runner uses substring matching rather than expert grading. Real Gemini benchmarking requires a configured Gemini backend and an authenticated short-lived user token, consumes provider tokens, and may incur cost.

Run `python docs/ai-benchmark/run.py --limit N` for an explicitly bounded sample or omit `--limit` to run the full dataset. The runner overwrites this report with measured results.
