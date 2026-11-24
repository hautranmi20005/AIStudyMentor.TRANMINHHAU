# ADR-0008: Deterministic Progress Analytics Without LLM Token Usage

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Progress metrics should be reproducible, explainable, and independent of generative-model variability or cost. Date-based summaries must use the learner’s timezone so that daily and weekly boundaries reflect the learner’s local calendar.

## 2. Decision Drivers & Alternatives Considered

- Derive metrics from persisted question, quiz, and attempt records.
- Use timezone-aware date boundaries for time-based aggregations.
- Do not call an LLM to calculate counts, scores, streaks, or other deterministic metrics.
- Alternatives considered: asking an LLM to infer progress, using server-local time for learner periods, and maintaining duplicate aggregate state without a clear consistency rule.

## 3. Decision Outcome & Justification

Compute progress analytics deterministically from persisted backend data. Apply the learner’s timezone when grouping or filtering by calendar period. Analytics calculations do not consume LLM tokens.

The current `DashboardService` calculates overall question and completed-quiz counts, averages attempt scores, and groups results by subject without an LLM call. It does not currently implement timezone-aware period aggregation; the inspected profile and dashboard data also do not show a persisted timezone field. The approved timezone policy is not yet implemented.

## 4. Consequences

- **Positive**: Results are reproducible and do not incur provider cost. Timezone-aware grouping matches learner-local periods.
- **Negative**: Timezone must be reliably collected and stored; daylight-saving transitions and missing/changed timezone values need defined handling and tests.
- Cached dashboard values require invalidation or expiry consistent with updates to the underlying learning records.

## 5. References & Code Artifacts

- [DashboardService.java](../../backend/src/main/java/com/aistudymentor/dashboard/service/DashboardService.java)
- [DashboardMetricsResponse.java](../../backend/src/main/java/com/aistudymentor/dashboard/dto/DashboardMetricsResponse.java)
- [StudentProfile.java](../../backend/src/main/java/com/aistudymentor/profile/entity/StudentProfile.java)
- [Dashboard schema objects](../../backend/src/main/resources/db/migration/V3__Question_And_Ai_Schema.sql)
