ALTER TABLE users
    ADD COLUMN auth_version INT NOT NULL DEFAULT 0;

ALTER TABLE student_profiles
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE student_profile_subjects (
    profile_id VARCHAR(36) NOT NULL,
    subject_order INT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    CONSTRAINT pk_student_profile_subjects PRIMARY KEY (profile_id, subject_order),
    CONSTRAINT fk_profile_subjects_profile FOREIGN KEY (profile_id)
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE TABLE mfa_recovery_codes (
    recovery_code_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_mfa_recovery_codes PRIMARY KEY (recovery_code_id),
    CONSTRAINT fk_mfa_recovery_codes_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_recovery_codes_user_id ON mfa_recovery_codes (user_id);

CREATE TABLE password_reset_tokens (
    reset_token_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (reset_token_id),
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
