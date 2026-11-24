package com.aistudymentor.notification.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProfileRecord> findProfiles() {
        return jdbcTemplate.query("""
                SELECT p.profile_id, u.dob, u.account_status, gc.status AS consent_status
                FROM student_profiles p
                JOIN users u ON u.user_id = p.user_id
                LEFT JOIN guardian_consents gc ON gc.user_id = u.user_id
                WHERE u.account_status = 'ACTIVE'
                """, (rs, rowNum) -> new ProfileRecord(
                UUID.fromString(rs.getString("profile_id")),
                rs.getDate("dob").toLocalDate(),
                rs.getString("account_status"),
                rs.getString("consent_status")));
    }

    public List<UUID> findBookmarkReminderProfiles(LocalDateTime viewedBefore) {
        return jdbcTemplate.query("""
                SELECT DISTINCT profile_id
                FROM question_bookmarks
                WHERE COALESCE(last_viewed_at, created_at) <= ?
                """, (rs, rowNum) -> UUID.fromString(rs.getString(1)), viewedBefore);
    }

    public List<UUID> findRepeatedMistakeProfiles(LocalDateTime since) {
        return jdbcTemplate.query("""
                SELECT qa.profile_id
                FROM quiz_attempts qa
                JOIN quiz_responses qr ON qr.attempt_id = qa.attempt_id
                JOIN quiz_questions qq ON qq.question_id = qr.question_id
                JOIN practice_quizzes pq ON pq.quiz_id = qa.quiz_id
                WHERE qa.end_time >= ?
                  AND qa.end_time IS NOT NULL
                  AND qr.is_correct = FALSE
                  AND qq.topic IS NOT NULL
                GROUP BY qa.profile_id, pq.subject, qq.topic
                HAVING COUNT(DISTINCT qa.attempt_id) >= 2
                """, (rs, rowNum) -> UUID.fromString(rs.getString(1)), since);
    }

    public List<UUID> findRecentlyActiveProfiles(LocalDateTime since) {
        return jdbcTemplate.query("""
                SELECT DISTINCT activity.profile_id
                FROM (
                    SELECT profile_id FROM questions WHERE created_at >= ?
                    UNION
                    SELECT profile_id FROM quiz_attempts WHERE end_time >= ? AND end_time IS NOT NULL
                ) activity
                """, (rs, rowNum) -> UUID.fromString(rs.getString(1)), since, since);
    }

    public record ProfileRecord(UUID profileId, LocalDate dateOfBirth, String accountStatus, String consentStatus) {
    }
}
