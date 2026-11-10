-- PostgreSQL equivalent of the core security hardening migration.

ALTER TABLE users ALTER COLUMN account_status DROP DEFAULT;
ALTER TABLE users ALTER COLUMN account_status TYPE VARCHAR(30);
ALTER TABLE users ADD CONSTRAINT chk_users_account_status
    CHECK (account_status IN ('ACTIVE', 'PENDING_CONSENT', 'SUSPENDED'));

ALTER TABLE mfa_factors ADD CONSTRAINT chk_mfa_factors_type
    CHECK (type IN ('TOTP'));
ALTER TABLE mfa_factors ALTER COLUMN secret TYPE VARCHAR(512);

ALTER TABLE guardian_consents ADD CONSTRAINT chk_guardian_consents_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'));

ALTER TABLE guardian_consents
    ADD COLUMN token_hash CHAR(64) NULL,
    ADD COLUMN token_expires_at TIMESTAMP(6) NULL,
    ADD COLUMN resend_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_sent_at TIMESTAMP(6) NULL,
    ADD COLUMN approved_ip VARCHAR(45) NULL;

CREATE INDEX idx_guardian_consents_token_hash ON guardian_consents (token_hash);

CREATE TABLE mfa_attempts (
    attempt_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    success BOOLEAN NOT NULL,
    ip VARCHAR(45) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mfa_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT fk_mfa_attempts_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_attempts_user_created ON mfa_attempts (user_id, created_at);
CREATE INDEX idx_mfa_attempts_ip_created ON mfa_attempts (ip, created_at);