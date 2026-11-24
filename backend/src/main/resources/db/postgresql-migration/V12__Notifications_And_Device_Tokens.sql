ALTER TABLE question_bookmarks
    ADD COLUMN last_viewed_at TIMESTAMP(6) NULL;
CREATE INDEX idx_bookmarks_last_viewed ON question_bookmarks (last_viewed_at, created_at);
CREATE INDEX idx_quiz_attempts_end_time ON quiz_attempts (end_time);
CREATE INDEX idx_quiz_responses_correct_attempt_question
    ON quiz_responses (is_correct, attempt_id, question_id);

CREATE TABLE device_tokens (
    token_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    app_version VARCHAR(50) NULL,
    last_seen TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_device_tokens PRIMARY KEY (token_id),
    CONSTRAINT uq_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);
CREATE INDEX idx_device_tokens_profile ON device_tokens (profile_id);

CREATE TABLE notification_preferences (
    profile_id VARCHAR(36) NOT NULL,
    bookmark_review_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    repeated_mistake_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_summary_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours_start TIME NULL,
    quiet_hours_end TIME NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (profile_id),
    CONSTRAINT fk_notification_preferences_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE TABLE notification_deliveries (
    delivery_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    period_key VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP(6) NULL,
    CONSTRAINT pk_notification_deliveries PRIMARY KEY (delivery_id),
    CONSTRAINT uq_notification_profile_type_period UNIQUE (profile_id, notification_type, period_key),
    CONSTRAINT fk_notification_deliveries_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);
CREATE INDEX idx_notification_deliveries_status_created
    ON notification_deliveries (status, created_at);
