-- =============================================================================
-- Migration: V1__Init_Core_Security_Schema.sql
-- Description: Core Auth, MFA, Guardian Consent, and Student Profile Schema
-- Database: MySQL 8.x
-- =============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- 2. MFA Factors Table
CREATE TABLE IF NOT EXISTS mfa_factors (
    factor_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    secret VARCHAR(512) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_mfa_factors PRIMARY KEY (factor_id),
    CONSTRAINT fk_mfa_factors_user FOREIGN KEY (user_id) 
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_factors_user_id ON mfa_factors (user_id);

-- 3. Guardian Consents Table (Under-13 Protection)
CREATE TABLE IF NOT EXISTS guardian_consents (
    consent_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    guardian_email VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_guardian_consents PRIMARY KEY (consent_id),
    CONSTRAINT uq_guardian_consents_user_id UNIQUE (user_id),
    CONSTRAINT fk_guardian_consents_user FOREIGN KEY (user_id) 
        REFERENCES users (user_id) ON DELETE CASCADE
);

-- 4. Student Profiles Table
CREATE TABLE IF NOT EXISTS student_profiles (
    profile_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    education_level VARCHAR(50) NULL,
    preferred_subject VARCHAR(100) NULL,
    learning_style VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_student_profiles PRIMARY KEY (profile_id),
    CONSTRAINT uq_student_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) 
        REFERENCES users (user_id) ON DELETE CASCADE
);
