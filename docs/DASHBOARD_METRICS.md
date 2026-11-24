# Dashboard Metrics Contract

This document defines the dashboard formulas and data contract before implementation. All metrics are scoped to the authenticated student's profile; never aggregate across profiles.

## Request and time semantics

`GET /api/v1/dashboard/metrics?range=7d|30d|all&tz=<IANA zone>`

- Default `range` is `30d`; default `tz` is `UTC`.
- `tz` must be a valid IANA `ZoneId`. Invalid ranges or zones return `400`.
- Timestamps in the legacy `DATETIME(6)` columns are naive `LocalDateTime` values written in the backend JVM's configured system timezone. Keep that timezone stable for the lifetime of the database; it cannot be inferred from `DATETIME`. The service converts requested-zone local calendar boundaries to instants, then to that storage timezone before querying. API window bounds are returned as UTC instants.
- `7d` and `30d` include the current local calendar day plus the prior 6 or 29 local calendar days. The lower bound is inclusive at local midnight; the upper bound is exclusive at the next local midnight. This preserves 23/25-hour days around daylight-saving transitions.
- `all` has no lower bound and ends at the next local midnight. Day/week grouping and month comparisons use the requested timezone.
- Weekly buckets start Monday (ISO week). `7d` and `30d` question trends are daily; `all` trends are weekly. Empty buckets within the selected period are returned with zero values.
- Quiz attempts are dated by `quiz_attempts.end_time`, the time grading completed. Question activity is dated by `questions.created_at`.

## Metric definitions

| Metric | Formula | Source and efficient query shape |
|---|---|---|
| Total questions | Count of questions created in the selected window. | `questions`: `COUNT(*) WHERE profile_id = ? AND created_at >= start AND created_at < end`; omit the lower predicate for `all`. |
| Questions per subject | Count questions by subject in the selected window. Return the top five subjects by count (ties by subject name), plus an `Other` bucket equal to all remaining counts. | `questions`: `GROUP BY subject`, profile and time predicates applied before aggregation. Top-N limit is configurable. |
| Questions trend | Count questions per requested-local date (`7d`/`30d`) or ISO week (`all`). Include zero-count buckets. | `questions`: profile/window filtered `GROUP BY` localized bucket. Convert each bucket boundary to UTC before querying. |
| Completed quiz attempts | Count every submitted attempt in the selected window; retakes count as separate attempts. | `quiz_attempts`: `profile_id = ? AND end_time IS NOT NULL AND end_time >= start AND end_time < end`; group/count in SQL. |
| Average quiz accuracy | Weighted accuracy: `100 * SUM(correct graded responses) / COUNT(graded responses)` for completed attempts in the selected window. Return `0` with no graded responses. | Join `quiz_responses` to `quiz_attempts`; filter by profile, non-null `end_time`, and `end_time` window; aggregate `is_correct` in SQL. |
| Accuracy trend | For each requested-local ISO week, `100 * correct graded responses / all graded responses` from attempts completed in that week. Return zero/empty buckets when there are no graded responses. | `quiz_responses` → `quiz_attempts`, filtered and grouped in SQL by the local week containing `end_time`. |
| Accuracy per subject | Weighted correct graded responses divided by all graded responses, as a percentage, grouped by the quiz subject. | `quiz_responses` → `quiz_attempts` → `practice_quizzes`; profile/window filter and `GROUP BY subject` in SQL. |
| Recent activity | The latest ten events in the selected window, ordered newest first: question created and quiz attempt submitted. No question/answer body is included. | Two indexed queries (`questions` and completed `quiz_attempts`), each fetching at most ten rows; merge and order those bounded result sets. |
| Weak topics | In the last 30 local calendar days, group graded quiz responses by quiz subject and question topic. A topic qualifies with at least the configured minimum graded responses and accuracy strictly below the configured maximum. Return `correctCount`, `gradedCount`, and accuracy (for example, `4 of 10 correct`, 40%). | `quiz_responses` → `quiz_attempts` → `practice_quizzes` and `quiz_questions`; window based on attempt `end_time`, grouped and filtered in SQL. |

Weak-topic defaults are `lookback-days=30`, `minimum-graded-answers=3`, and `maximum-accuracy-percent=60`, all configurable under `dashboard.weak-topic` in `application.yml`. The upper accuracy comparison is strict: exactly 60% does not qualify.

## Deterministic insights and empty state

- Insights are pure templates over returned aggregates; they never invoke Gemini or another AI provider.
- Emit an insight only when all values it references have non-empty supporting buckets. For example, compare the earliest and latest non-empty weekly accuracy buckets for a subject and render an improvement/decline only when both buckets have graded responses. Include the bucket labels and percentages in the insight payload.
- Weak-topic insights use the same returned counts and accuracy as the weak-topic rows. Never infer a trend from a single data point or generate a claim without its denominators.
- `hasActivity` is false only when the selected window has no questions and no completed quiz attempts. The client then displays an onboarding empty state instead of zero-valued charts.

## Indexes and aggregation constraints

Use the existing profile/time indexes where applicable and add a migration for missing access paths:

- `questions(profile_id, created_at, subject)` for profile-scoped date and subject aggregates.
- `quiz_attempts(profile_id, end_time)` for completed attempt windows.
- `quiz_responses(attempt_id, question_id, is_correct)` for completed-attempt grading aggregates.
- `quiz_questions(quiz_id, topic)` and `practice_quizzes(profile_id, subject)` for weak-topic/subject joins.

Metrics must be aggregated by the database; do not load a student's questions, quizzes, or responses into application memory. The response is uncached by default. Add Redis caching only after measured dashboard latency warrants it and a cache invalidation strategy exists for new questions and submitted attempts.

## API response shape

The response contains the resolved `range`, IANA `timezone`, UTC window bounds, `hasActivity`, total questions, top-N subject counts plus `Other`, question trend buckets, completed attempt count, weighted accuracy, weekly accuracy buckets, per-subject accuracy, at most ten recent activity events, qualifying weak topics with numerator/denominator, and deterministic insight records with their source period and values.
