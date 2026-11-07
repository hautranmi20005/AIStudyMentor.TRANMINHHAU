-- =============================================================================
-- Migration: V2__Fix_Core_Security.sql
-- Description: Security hardening, U13 token hash & audit, MFA attempts audit/rate-limiting
-- Database: MySQL 8.x
-- =============================================================================

-- 1. Hardening users.account_status: Remove default in DB (managed by service), add CHECK constraint
ALTER TABLE users ALTER COLUMN account_status DROP DEFAULT;
ALTER TABLE users MODIFY COLUMN account_status VARCHAR(30) NOT NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_account_status 
    CHECK (account_status IN ('ACTIVE', 'PENDING_CONSENT', 'SUSPENDED'));

-- 2. Hardening mfa_factors.type: CHECK constraint for TOTP (Decision D1: TOTP only)
ALTER TABLE mfa_factors ADD CONSTRAINT chk_mfa_factors_type 
    CHECK (type IN ('TOTP'));

-- Expand secret length if needed for AES-GCM encrypted payload
ALTER TABLE mfa_factors MODIFY COLUMN secret VARCHAR(512) NOT NULL;

-- 3. Hardening guardian_consents: CHECK constraint for status (PENDING, APPROVED, REJECTED, EXPIRED)
ALTER TABLE guardian_consents ADD CONSTRAINT chk_guardian_consents_status 
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'));

-- 4. Guardian Consents: Add single-use signed token hash (SHA-256), expiry (72h), resend count & audit fields
ALTER TABLE guardian_consents 
    ADD COLUMN token_hash CHAR(64) NULL AFTER status,
    ADD COLUMN token_expires_at DATETIME(6) NULL AFTER token_hash,
    ADD COLUMN resend_count INT NOT NULL DEFAULT 0 AFTER token_expires_at,
    ADD COLUMN last_sent_at DATETIME(6) NULL AFTER resend_count,
    ADD COLUMN approved_ip VARCHAR(45) NULL AFTER last_sent_at;

CREATE INDEX idx_guardian_consents_token_hash ON guardian_consents (token_hash);

-- 5. Create mfa_attempts table for rate limiting (brute-force defense) and security auditing
CREATE TABLE IF NOT EXISTS mfa_attempts (
    attempt_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    success BOOLEAN NOT NULL,
    ip VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_mfa_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT fk_mfa_attempts_user FOREIGN KEY (user_id) 
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mfa_attempts_user_created ON mfa_attempts (user_id, created_at);
CREATE INDEX idx_mfa_attempts_ip_created ON mfa_attempts (ip, created_at);
