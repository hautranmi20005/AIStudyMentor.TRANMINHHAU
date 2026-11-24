CREATE TABLE answer_reports (
    id VARCHAR(36) NOT NULL,
    answer_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    comment VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    CONSTRAINT pk_answer_reports PRIMARY KEY (id),
    CONSTRAINT fk_answer_reports_answer FOREIGN KEY (answer_id)
        REFERENCES answers (answer_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_reports_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_answer_reports_answer_created ON answer_reports (answer_id, created_at);
CREATE INDEX idx_answer_reports_profile_created ON answer_reports (profile_id, created_at);
