-- =============================================================================
-- Migration: V3__Question_And_Ai_Schema.sql
-- Description: Core Q&A, Attachments, Answers, Bookmarks, and AI Telemetry Logs
-- Database: MySQL 8.x
-- =============================================================================

-- 1. Questions Table (NO is_bookmarked column; ownership bound to student_profiles.profile_id)
CREATE TABLE IF NOT EXISTS questions (
    question_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    question_text TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_questions PRIMARY KEY (question_id),
    CONSTRAINT fk_questions_profile FOREIGN KEY (profile_id) 
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_questions_profile_created ON questions (profile_id, created_at);

-- 2. Question Attachments Table (Firebase Storage object references)
CREATE TABLE IF NOT EXISTS question_attachments (
    attachment_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    firebase_storage_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_question_attachments PRIMARY KEY (attachment_id),
    CONSTRAINT fk_question_attachments_question FOREIGN KEY (question_id) 
        REFERENCES questions (question_id) ON DELETE CASCADE
);

CREATE INDEX idx_question_attachments_question_id ON question_attachments (question_id);

-- 3. AI Generation Logs Table (Audit & Telemetry, bound to student_profiles)
CREATE TABLE IF NOT EXISTS ai_generation_logs (
    log_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NULL,
    quiz_id VARCHAR(36) NULL,
    model_id VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    latency_first_token_ms BIGINT NULL,
    latency_total_ms BIGINT NULL,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ai_generation_logs PRIMARY KEY (log_id),
    CONSTRAINT fk_ai_logs_profile FOREIGN KEY (profile_id) 
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_logs_profile_created ON ai_generation_logs (profile_id, created_at);
CREATE INDEX idx_ai_logs_question_id ON ai_generation_logs (question_id);
CREATE INDEX idx_ai_logs_quiz_id ON ai_generation_logs (quiz_id);

-- 4. Answers Table (Raw text + strictly validated structured JSON output)
CREATE TABLE IF NOT EXISTS answers (
    answer_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    raw_content TEXT NULL,
    structured_json TEXT NOT NULL,
    is_alternative BOOLEAN NOT NULL DEFAULT FALSE,
    ai_log_id VARCHAR(36) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_answers PRIMARY KEY (answer_id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) 
        REFERENCES questions (question_id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_ai_log FOREIGN KEY (ai_log_id) 
        REFERENCES ai_generation_logs (log_id) ON DELETE SET NULL
);

CREATE INDEX idx_answers_question_id ON answers (question_id);
CREATE INDEX idx_answers_ai_log_id ON answers (ai_log_id);

-- 5. Question Bookmarks Table (Dedicated table; no boolean column in questions)
CREATE TABLE IF NOT EXISTS question_bookmarks (
    bookmark_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_question_bookmarks PRIMARY KEY (bookmark_id),
    CONSTRAINT uq_bookmarks_profile_question UNIQUE (profile_id, question_id),
    CONSTRAINT fk_bookmarks_profile FOREIGN KEY (profile_id) 
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_question FOREIGN KEY (question_id) 
        REFERENCES questions (question_id) ON DELETE CASCADE
);

CREATE INDEX idx_bookmarks_profile_created ON question_bookmarks (profile_id, created_at);
