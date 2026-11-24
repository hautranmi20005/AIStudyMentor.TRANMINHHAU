# ADR-0005: Redis Exact-Match Answer Cache and Stampede Protection

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Repeated identical study questions can trigger redundant provider calls, latency, and token charges. A cache must distinguish requests whose generated answers can differ while avoiding raw question text in Redis keys and remaining safe when Redis is unavailable.

## 2. Decision Drivers & Alternatives Considered

- Cache only exact matches using a stable canonical request fingerprint and a cryptographic hash for the key.
- Include answer-affecting provider/model and prompt/schema versions in the fingerprint.
- Expire answer entries after seven days.
- Protect concurrent misses from causing a provider-call stampede.
- Degrade gracefully by bypassing the cache when Redis is unavailable.
- Alternatives considered: approximate/fuzzy matching, unbounded retention, raw prompt text in keys, and making Redis availability a prerequisite for answering.

## 3. Decision Outcome & Justification

Use Redis for a seven-day exact-match answer cache. Compute a stable canonical fingerprint from the complete answer-affecting request, hash it for the Redis key, and use a coordination mechanism for concurrent cache misses. On Redis failure, continue through the normal provider path without cache rather than returning stale data or failing solely because the cache is down.

The answer cache is not present in the current implementation. Redis currently supports other functions and Spring caches profile/dashboard data; `QuestionService` logs `cacheHit=false`. The inspected code does not establish answer-key canonicalization, seven-day answer TTL, stampede protection, or answer-cache failure behavior.

## 4. Consequences

- **Positive**: Exact reuse can reduce repeated provider calls, latency, and token consumption; hashed keys avoid exposing prompt text in key names.
- **Negative**: Incorrect canonicalization or omitted model/prompt inputs can return an unsuitable answer. Cache coordination adds Redis operations and requires careful timeout handling.
- Cache entries must not bypass ownership checks or mix user-specific context. Cache contents and invalidation require explicit tests before the policy is considered implemented.

## 5. References & Code Artifacts

- [RedisCacheConfig.java](../../backend/src/main/java/com/aistudymentor/config/RedisCacheConfig.java)
- [QuestionService.java](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- [AiGenerationLog.java](../../backend/src/main/java/com/aistudymentor/ai/entity/AiGenerationLog.java)
- [AI integration gap analysis](../audit/AI_INTEGRATION_GAP_ANALYSIS.md)
- [Requirements traceability matrix](../audit/REQUIREMENTS_TRACEABILITY_MATRIX.md)
