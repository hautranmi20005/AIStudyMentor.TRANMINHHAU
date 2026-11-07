# AI Orchestrator Specification (Gemini) v1.1

## 1. Pipeline
1. **Safety Check**: Validate text and image size.
2. **Cache**: Exact-match lookup in Redis `qa:v1:{model}:{promptVersion}:{hash}`.
3. **Adapter**: Use Spring Boot adapter for Google Gemini.
4. **Prompting**: Send strictly formatted system instructions demanding JSON output.
5. **Output**: Parse Gemini JSON. Validate required fields (`steps`, `finalAnswer`, etc.).
6. **Telemetry**: Log input/output tokens, latency (first token vs complete).

## 2. TBD Values
- **Gemini Model ID**: e.g., `gemini-1.5-flash`.
- **API Version**: e.g., `v1beta`.

## 3. Quiz Generation
- The Orchestrator also handles Quiz Generation requests, outputting JSON arrays of questions, options, and correct indexes. Correct indexes are stripped from DTOs before reaching the client.
