CREATE TABLE account_deletion_jobs (
    user_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    user_id_hash VARCHAR(64) NOT NULL,
    storage_paths_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_account_deletion_jobs PRIMARY KEY (user_id)
);

CREATE TABLE account_deletion_audit (
    user_id_hash VARCHAR(64) NOT NULL,
    deleted_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_account_deletion_audit PRIMARY KEY (user_id_hash)
);

CREATE TABLE guardian_consent_evidence (
    user_id_hash VARCHAR(64) NOT NULL,
    consent_status VARCHAR(30) NOT NULL,
    consent_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    CONSTRAINT pk_guardian_consent_evidence PRIMARY KEY (user_id_hash)
);
CREATE INDEX idx_guardian_consent_evidence_expiry ON guardian_consent_evidence (expires_at);
