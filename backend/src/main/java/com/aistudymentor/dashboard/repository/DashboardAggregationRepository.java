package com.aistudymentor.dashboard.repository;

import com.aistudymentor.dashboard.service.DashboardBucket;
import com.aistudymentor.dashboard.service.DashboardTimeWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DashboardAggregationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public long countQuestions(UUID profileId, DashboardTimeWindow window) {
        String sql = "SELECT COUNT(*) FROM questions q WHERE q.profile_id = :profileId"
                + windowFilter("q.created_at", window);
        return jdbc.queryForObject(sql, windowParameters(profileId, window), Long.class);
    }

    public List<SubjectCount> questionCounts(UUID profileId, DashboardTimeWindow window, int limit) {
        String sql = """
                SELECT q.subject, COUNT(*) AS question_count
                FROM questions q
                WHERE q.profile_id = :profileId
                """
                + windowFilter("q.created_at", window)
                + """
                GROUP BY q.subject
                ORDER BY question_count DESC, q.subject ASC
                LIMIT :rowLimit
                """;
        MapSqlParameterSource parameters = windowParameters(profileId, window)
                .addValue("rowLimit", limit);
        return jdbc.query(sql, parameters, (rs, row) ->
                new SubjectCount(rs.getString("subject"), rs.getLong("question_count")));
    }

    public List<BucketCount> questionTrend(
            UUID profileId,
            List<DashboardBucket> buckets
    ) {
        if (buckets.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource parameters = bucketParameters(buckets)
                .addValue("profileId", profileId.toString());
        String sql = """
                SELECT bucket.bucket_label, COUNT(q.question_id) AS event_count
                FROM (
                """
                + bucketValues(buckets.size())
                + """
                ) bucket
                LEFT JOIN questions q
                  ON q.profile_id = :profileId
                 AND q.created_at >= bucket.bucket_start
                 AND q.created_at < bucket.bucket_end
                GROUP BY bucket.bucket_label
                ORDER BY bucket.bucket_label
                """;
        return jdbc.query(sql, parameters, (rs, row) ->
                new BucketCount(rs.getString("bucket_label"), rs.getLong("event_count")));
    }

    public long countCompletedAttempts(UUID profileId, DashboardTimeWindow window) {
        String sql = "SELECT COUNT(*) FROM quiz_attempts a WHERE a.profile_id = :profileId"
                + windowFilter("a.end_time", window);
        return jdbc.queryForObject(sql, windowParameters(profileId, window), Long.class);
    }

    public Map<String, Long> completedAttemptsBySubject(UUID profileId, DashboardTimeWindow window) {
        String sql = """
                SELECT q.subject, COUNT(*) AS attempt_count
                FROM quiz_attempts a
                JOIN practice_quizzes q ON q.quiz_id = a.quiz_id
                WHERE a.profile_id = :profileId
                """
                + windowFilter("a.end_time", window)
                + """
                GROUP BY q.subject
                """;
        Map<String, Long> result = new HashMap<>();
        jdbc.query(sql, windowParameters(profileId, window), rs -> {
            result.put(rs.getString("subject"), rs.getLong("attempt_count"));
        });
        return result;
    }

    public AccuracyCounts overallAccuracy(UUID profileId, DashboardTimeWindow window) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END), 0) AS correct_count,
                       COUNT(r.response_id) AS graded_count
                FROM quiz_attempts a
                JOIN quiz_responses r ON r.attempt_id = a.attempt_id
                WHERE a.profile_id = :profileId
                """
                + windowFilter("a.end_time", window);
        return jdbc.queryForObject(sql, windowParameters(profileId, window), (rs, row) ->
                new AccuracyCounts(rs.getLong("correct_count"), rs.getLong("graded_count")));
    }

    public List<BucketAccuracy> accuracyTrend(
            UUID profileId,
            DashboardTimeWindow window,
            List<DashboardBucket> buckets
    ) {
        if (buckets.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource parameters = bucketParameters(buckets)
                .addValue("profileId", profileId.toString())
                .addValue("windowEnd", window.storageEndExclusive());
        addWindowStart(parameters, window);
        String sql = """
                SELECT bucket.bucket_label,
                       COALESCE(SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END), 0) AS correct_count,
                       COUNT(r.response_id) AS graded_count
                FROM (
                """
                + bucketValues(buckets.size())
                + """
                ) bucket
                LEFT JOIN quiz_attempts a
                  ON a.profile_id = :profileId
                 AND a.end_time < :windowEnd
                """
                + (window.storageStartInclusive() == null ? "" : " AND a.end_time >= :windowStart\n")
                + """
                 AND a.end_time >= bucket.bucket_start
                 AND a.end_time < bucket.bucket_end
                LEFT JOIN quiz_responses r ON r.attempt_id = a.attempt_id
                GROUP BY bucket.bucket_label
                ORDER BY bucket.bucket_label
                """;
        return jdbc.query(sql, parameters, (rs, row) -> new BucketAccuracy(
                rs.getString("bucket_label"),
                rs.getLong("correct_count"),
                rs.getLong("graded_count")));
    }

    public List<SubjectAccuracyCount> subjectAccuracy(
            UUID profileId,
            DashboardTimeWindow window
    ) {
        String sql = """
                SELECT q.subject,
                       SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END) AS correct_count,
                       COUNT(r.response_id) AS graded_count
                FROM quiz_attempts a
                JOIN practice_quizzes q ON q.quiz_id = a.quiz_id
                JOIN quiz_responses r ON r.attempt_id = a.attempt_id
                WHERE a.profile_id = :profileId
                """
                + windowFilter("a.end_time", window)
                + """
                GROUP BY q.subject
                ORDER BY q.subject ASC
                """;
        return jdbc.query(sql, windowParameters(profileId, window), (rs, row) -> new SubjectAccuracyCount(
                rs.getString("subject"),
                rs.getLong("correct_count"),
                rs.getLong("graded_count")));
    }

    public List<SubjectBucketAccuracy> subjectAccuracyTrend(
            UUID profileId,
            DashboardTimeWindow window,
            List<DashboardBucket> buckets
    ) {
        if (buckets.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource parameters = bucketParameters(buckets)
                .addValue("profileId", profileId.toString())
                .addValue("windowEnd", window.storageEndExclusive());
        addWindowStart(parameters, window);
        String sql = """
                SELECT bucket.bucket_label, q.subject,
                       SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END) AS correct_count,
                       COUNT(r.response_id) AS graded_count
                FROM (
                """
                + bucketValues(buckets.size())
                + """
                ) bucket
                JOIN quiz_attempts a
                  ON a.profile_id = :profileId
                 AND a.end_time < :windowEnd
                """
                + (window.storageStartInclusive() == null ? "" : " AND a.end_time >= :windowStart\n")
                + """
                 AND a.end_time >= bucket.bucket_start
                 AND a.end_time < bucket.bucket_end
                JOIN practice_quizzes q ON q.quiz_id = a.quiz_id
                JOIN quiz_responses r ON r.attempt_id = a.attempt_id
                GROUP BY bucket.bucket_label, q.subject
                ORDER BY q.subject ASC, bucket.bucket_label ASC
                """;
        return jdbc.query(sql, parameters, (rs, row) -> new SubjectBucketAccuracy(
                rs.getString("bucket_label"),
                rs.getString("subject"),
                rs.getLong("correct_count"),
                rs.getLong("graded_count")));
    }

    public List<ActivityRecord> recentQuestions(UUID profileId, DashboardTimeWindow window) {
        String sql = """
                SELECT q.question_id AS event_id, 'QUESTION_CREATED' AS event_type,
                       q.subject, CAST(NULL AS SIGNED) AS score, q.created_at AS occurred_at
                FROM questions q
                WHERE q.profile_id = :profileId
                """
                + windowFilter("q.created_at", window)
                + """
                ORDER BY q.created_at DESC
                LIMIT 10
                """;
        return jdbc.query(sql, windowParameters(profileId, window), (rs, row) -> activityRecord(rs));
    }

    public List<ActivityRecord> recentQuizAttempts(UUID profileId, DashboardTimeWindow window) {
        String sql = """
                SELECT a.attempt_id AS event_id, 'QUIZ_COMPLETED' AS event_type,
                       q.subject, a.score, a.end_time AS occurred_at
                FROM quiz_attempts a
                JOIN practice_quizzes q ON q.quiz_id = a.quiz_id
                WHERE a.profile_id = :profileId
                """
                + windowFilter("a.end_time", window)
                + """
                ORDER BY a.end_time DESC
                LIMIT 10
                """;
        return jdbc.query(sql, windowParameters(profileId, window), (rs, row) -> activityRecord(rs));
    }

    public List<TopicAccuracyCount> weakTopicAccuracy(
            UUID profileId,
            DashboardTimeWindow window,
            int minimumGradedAnswers,
            double maximumAccuracyPercent
    ) {
        String sql = """
                SELECT q.subject, qq.topic,
                       SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END) AS correct_count,
                       COUNT(r.response_id) AS graded_count
                FROM quiz_attempts a
                JOIN practice_quizzes q ON q.quiz_id = a.quiz_id
                JOIN quiz_responses r ON r.attempt_id = a.attempt_id
                JOIN quiz_questions qq ON qq.question_id = r.question_id
                WHERE a.profile_id = :profileId
                """
                + windowFilter("a.end_time", window)
                + """
                  AND qq.topic IS NOT NULL
                  AND TRIM(qq.topic) <> ''
                GROUP BY q.subject, qq.topic
                HAVING COUNT(r.response_id) >= :minimumGradedAnswers
                   AND SUM(CASE WHEN r.is_correct = TRUE THEN 1 ELSE 0 END) * 100.0
                       / COUNT(r.response_id) < :maximumAccuracyPercent
                ORDER BY correct_count * 1.0 / graded_count ASC, graded_count DESC,
                         q.subject ASC, qq.topic ASC
                """;
        MapSqlParameterSource parameters = windowParameters(profileId, window)
                .addValue("minimumGradedAnswers", minimumGradedAnswers)
                .addValue("maximumAccuracyPercent", maximumAccuracyPercent);
        return jdbc.query(sql, parameters, (rs, row) -> new TopicAccuracyCount(
                rs.getString("subject"),
                rs.getString("topic"),
                rs.getLong("correct_count"),
                rs.getLong("graded_count")));
    }

    public Optional<LocalDateTime> firstActivity(UUID profileId, DashboardTimeWindow window) {
        String sql = """
                SELECT MIN(activity.event_time)
                FROM (
                    SELECT q.created_at AS event_time
                    FROM questions q
                    WHERE q.profile_id = :profileId
                    UNION ALL
                    SELECT a.end_time AS event_time
                    FROM quiz_attempts a
                    WHERE a.profile_id = :profileId AND a.end_time IS NOT NULL
                ) activity
                WHERE activity.event_time < :windowEnd
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("profileId", profileId.toString())
                .addValue("windowEnd", window.storageEndExclusive());
        return jdbc.query(sql, parameters, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            Timestamp timestamp = rs.getTimestamp(1);
            return Optional.ofNullable(timestamp == null ? null : timestamp.toLocalDateTime());
        });
    }

    private ActivityRecord activityRecord(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("occurred_at");
        Number score = (Number) rs.getObject("score");
        return new ActivityRecord(
                rs.getString("event_id"),
                rs.getString("event_type"),
                rs.getString("subject"),
                score == null ? null : score.intValue(),
                timestamp == null ? null : timestamp.toLocalDateTime());
    }

    private MapSqlParameterSource windowParameters(UUID profileId, DashboardTimeWindow window) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("profileId", profileId.toString())
                .addValue("windowEnd", window.storageEndExclusive());
        addWindowStart(parameters, window);
        return parameters;
    }

    private void addWindowStart(MapSqlParameterSource parameters, DashboardTimeWindow window) {
        if (window.storageStartInclusive() != null) {
            parameters.addValue("windowStart", window.storageStartInclusive());
        }
    }

    private String windowFilter(String timestampColumn, DashboardTimeWindow window) {
        String start = window.storageStartInclusive() == null
                ? ""
                : " AND " + timestampColumn + " >= :windowStart";
        return start + " AND " + timestampColumn + " < :windowEnd ";
    }

    private MapSqlParameterSource bucketParameters(List<DashboardBucket> buckets) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        for (int i = 0; i < buckets.size(); i++) {
            DashboardBucket bucket = buckets.get(i);
            parameters.addValue("bucketLabel" + i, bucket.label());
            parameters.addValue("bucketStart" + i, bucket.storageStartInclusive());
            parameters.addValue("bucketEnd" + i, bucket.storageEndExclusive());
        }
        return parameters;
    }

    private String bucketValues(int bucketCount) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            rows.add("SELECT :bucketLabel" + i + " AS bucket_label, :bucketStart" + i
                    + " AS bucket_start, :bucketEnd" + i + " AS bucket_end");
        }
        return String.join("\nUNION ALL\n", rows);
    }

    public record SubjectCount(String subject, long count) {
    }

    public record BucketCount(String label, long count) {
    }

    public record AccuracyCounts(long correctCount, long gradedCount) {
    }

    public record BucketAccuracy(String label, long correctCount, long gradedCount) {
    }

    public record SubjectAccuracyCount(String subject, long correctCount, long gradedCount) {
    }

    public record SubjectBucketAccuracy(
            String label,
            String subject,
            long correctCount,
            long gradedCount
    ) {
    }

    public record ActivityRecord(
            String id,
            String type,
            String subject,
            Integer score,
            LocalDateTime occurredAt
    ) {
    }

    public record TopicAccuracyCount(String subject, String topic, long correctCount, long gradedCount) {
    }
}
