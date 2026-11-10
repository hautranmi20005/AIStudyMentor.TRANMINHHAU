-- =============================================================================
-- Migration: V4__Quiz_Schema.sql
-- Description: Practice Quizzes, Questions, Options, Attempts, and Server-Side Graded Responses
-- Database: MySQL 8.x
-- =============================================================================

-- 1. Practice Quizzes Table
CREATE TABLE IF NOT EXISTS practice_quizzes (
    quiz_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_practice_quizzes PRIMARY KEY (quiz_id),
    CONSTRAINT fk_quizzes_profile FOREIGN KEY (profile_id) 
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_quizzes_profile_created ON practice_quizzes (profile_id, created_at);

-- 2. Quiz Questions Table (correct_answer stored strictly server-side, never exposed to DTOs)
CREATE TABLE IF NOT EXISTS quiz_questions (
    question_id VARCHAR(36) NOT NULL,
    quiz_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    prompt TEXT NOT NULL,
    correct_answer VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_quiz_questions PRIMARY KEY (question_id),
    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id) 
        REFERENCES practice_quizzes (quiz_id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions (quiz_id);

-- 3. Quiz Options Table (for MCQ questions)
CREATE TABLE IF NOT EXISTS quiz_options (
    option_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    option_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_quiz_options PRIMARY KEY (option_id),
    CONSTRAINT fk_quiz_options_question FOREIGN KEY (question_id) 
        REFERENCES quiz_questions (question_id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_options_question_id ON quiz_options (question_id);

-- 4. Quiz Attempts Table
CREATE TABLE IF NOT EXISTS quiz_attempts (
    attempt_id VARCHAR(36) NOT NULL,
    quiz_id VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    start_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    end_time DATETIME(6) NULL,
    score INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_quiz_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) 
        REFERENCES practice_quizzes (quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempts_profile FOREIGN KEY (profile_id) 
        REFERENCES student_profiles (profile_id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_attempts_profile_created ON quiz_attempts (profile_id, created_at);
CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts (quiz_id);

-- 5. Quiz Responses Table (Server-side evaluated responses)
CREATE TABLE IF NOT EXISTS quiz_responses (
    response_id VARCHAR(36) NOT NULL,
    attempt_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    selected_option_id VARCHAR(36) NULL,
    subjective_text TEXT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_quiz_responses PRIMARY KEY (response_id),
    CONSTRAINT fk_quiz_responses_attempt FOREIGN KEY (attempt_id) 
        REFERENCES quiz_attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_responses_question FOREIGN KEY (question_id) 
        REFERENCES quiz_questions (question_id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_responses_attempt_id ON quiz_responses (attempt_id);
CREATE INDEX idx_quiz_responses_question_id ON quiz_responses (question_id);
