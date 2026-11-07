# Database Design v1.2 (MySQL 8.x)

This document represents the authoritative physical database schema, strictly synchronized with Flyway migrations `V1__Init_Core_Security_Schema.sql`, `V2__Fix_Core_Security.sql`, `V3__Question_And_Ai_Schema.sql`, and `V4__Quiz_Schema.sql`.

Engine: `InnoDB` | Charset: `utf8mb4` | Collation: `utf8mb4_unicode_ci` | Primary Keys: `VARCHAR(36)` (UUID)

---

## 1. Core Security & Authentication

### 1.1 `users`
Represents student account identity.
* `user_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `email` VARCHAR(150) NOT NULL UNIQUE
* `password_hash` VARCHAR(255) NOT NULL
* `dob` DATE NOT NULL (Used to determine U13 status at registration)
* `account_status` VARCHAR(30) NOT NULL CHECK (`account_status` IN ('ACTIVE', 'PENDING_CONSENT', 'SUSPENDED'))
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)

### 1.2 `mfa_factors`
Multi-Factor Authentication configurations (TOTP only per Decision D1).
* `factor_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `user_id` VARCHAR(36) NOT NULL FK -> `users(user_id)` ON DELETE CASCADE
* `type` VARCHAR(30) NOT NULL CHECK (`type` IN ('TOTP'))
* `secret` VARCHAR(512) NOT NULL (AES-GCM encrypted payload using `MFA_ENCRYPTION_KEY`)
* `is_verified` BOOLEAN NOT NULL DEFAULT FALSE
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_mfa_factors_user_id` (`user_id`)

### 1.3 `mfa_attempts`
Audit log and brute-force tracking (Rate limit: max 5 failed attempts per 15 minutes).
* `attempt_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `user_id` VARCHAR(36) NOT NULL FK -> `users(user_id)` ON DELETE CASCADE
* `success` BOOLEAN NOT NULL
* `ip` VARCHAR(45) NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* **Indexes**: 
  - `idx_mfa_attempts_user_created` (`user_id`, `created_at`)
  - `idx_mfa_attempts_ip_created` (`ip`, `created_at`)

### 1.4 `guardian_consents`
Under-13 parent/guardian consent lifecycle (Decision D2: 72h signed email loop).
* `consent_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `user_id` VARCHAR(36) NOT NULL UNIQUE FK -> `users(user_id)` ON DELETE CASCADE
* `guardian_email` VARCHAR(150) NOT NULL
* `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
* `token_hash` CHAR(64) NULL (SHA-256 hash of single-use verification token)
* `token_expires_at` DATETIME(6) NULL
* `resend_count` INT NOT NULL DEFAULT 0
* `last_sent_at` DATETIME(6) NULL
* `approved_ip` VARCHAR(45) NULL
* `verified_at` DATETIME(6) NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_guardian_consents_token_hash` (`token_hash`)

### 1.5 `student_profiles`
Student academic profile and preferences. All educational data is owned by `profile_id`.
* `profile_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `user_id` VARCHAR(36) NOT NULL UNIQUE FK -> `users(user_id)` ON DELETE CASCADE
* `education_level` VARCHAR(50) NULL
* `preferred_subject` VARCHAR(100) NULL
* `learning_style` VARCHAR(50) NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)

---

## 2. Core Q&A & AI Engine

### 2.1 `questions`
Questions submitted by students (text and/or image path).
* `question_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `profile_id` VARCHAR(36) NOT NULL FK -> `student_profiles(profile_id)` ON DELETE CASCADE
* `subject` VARCHAR(100) NOT NULL
* `question_text` TEXT NULL
* `status` VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED'
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_questions_profile_created` (`profile_id`, `created_at`)
* *Note*: `is_bookmarked` is removed entirely per Decision D7.

### 2.2 `question_attachments`
Direct Firebase Storage object pointers uploaded by the Android client.
* `attachment_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `question_id` VARCHAR(36) NOT NULL FK -> `questions(question_id)` ON DELETE CASCADE
* `firebase_storage_path` VARCHAR(500) NOT NULL
* `file_type` VARCHAR(50) NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_question_attachments_question_id` (`question_id`)

### 2.3 `ai_generation_logs`
Audit and telemetry for Google Gemini LLM invocations.
* `log_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `profile_id` VARCHAR(36) NOT NULL FK -> `student_profiles(profile_id)` ON DELETE CASCADE
* `question_id` VARCHAR(36) NULL
* `quiz_id` VARCHAR(36) NULL
* `model_id` VARCHAR(100) NOT NULL
* `prompt_version` VARCHAR(50) NOT NULL
* `input_tokens` INT NULL
* `output_tokens` INT NULL
* `latency_first_token_ms` BIGINT NULL
* `latency_total_ms` BIGINT NULL
* `cache_hit` BOOLEAN NOT NULL DEFAULT FALSE
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Indexes**:
  - `idx_ai_logs_profile_created` (`profile_id`, `created_at`)
  - `idx_ai_logs_question_id` (`question_id`)
  - `idx_ai_logs_quiz_id` (`quiz_id`)

### 2.4 `answers`
Structured explanations generated by AI.
* `answer_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `question_id` VARCHAR(36) NOT NULL FK -> `questions(question_id)` ON DELETE CASCADE
* `raw_content` TEXT NULL
* `structured_json` TEXT NOT NULL (Conforms to `docs/ai_response.schema.json`)
* `is_alternative` BOOLEAN NOT NULL DEFAULT FALSE (FR-05)
* `ai_log_id` VARCHAR(36) NULL FK -> `ai_generation_logs(log_id)` ON DELETE SET NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Indexes**:
  - `idx_answers_question_id` (`question_id`)
  - `idx_answers_ai_log_id` (`ai_log_id`)

### 2.5 `question_bookmarks`
Saved questions by student profile.
* `bookmark_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `profile_id` VARCHAR(36) NOT NULL FK -> `student_profiles(profile_id)` ON DELETE CASCADE
* `question_id` VARCHAR(36) NOT NULL FK -> `questions(question_id)` ON DELETE CASCADE
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Constraints**: UNIQUE (`profile_id`, `question_id`)
* **Index**: `idx_bookmarks_profile_created` (`profile_id`, `created_at`)

---

## 3. Practice Quizzes & Progress Dashboard

### 3.1 `practice_quizzes`
Generated practice quizzes (FR-07).
* `quiz_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `profile_id` VARCHAR(36) NOT NULL FK -> `student_profiles(profile_id)` ON DELETE CASCADE
* `subject` VARCHAR(100) NOT NULL
* `status` VARCHAR(30) NOT NULL DEFAULT 'GENERATED'
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_quizzes_profile_created` (`profile_id`, `created_at`)

### 3.2 `quiz_questions`
Individual questions within a quiz.
* `question_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `quiz_id` VARCHAR(36) NOT NULL FK -> `practice_quizzes(quiz_id)` ON DELETE CASCADE
* `type` VARCHAR(30) NOT NULL (MCQ, SHORT_ANSWER)
* `prompt` TEXT NOT NULL
* `correct_answer` VARCHAR(255) NOT NULL (**Strictly server-side; NEVER exposed in any client DTO**)
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_quiz_questions_quiz_id` (`quiz_id`)

### 3.3 `quiz_options`
Options for multiple-choice quiz questions.
* `option_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `question_id` VARCHAR(36) NOT NULL FK -> `quiz_questions(question_id)` ON DELETE CASCADE
* `option_text` VARCHAR(255) NOT NULL
* `option_order` INT NOT NULL
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Index**: `idx_quiz_options_question_id` (`question_id`)

### 3.4 `quiz_attempts`
Student attempts at completing a practice quiz.
* `attempt_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `quiz_id` VARCHAR(36) NOT NULL FK -> `practice_quizzes(quiz_id)` ON DELETE CASCADE
* `profile_id` VARCHAR(36) NOT NULL FK -> `student_profiles(profile_id)` ON DELETE CASCADE
* `start_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `end_time` DATETIME(6) NULL
* `score` INT NULL (Calculated server-side after submission)
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Indexes**:
  - `idx_quiz_attempts_profile_created` (`profile_id`, `created_at`)
  - `idx_quiz_attempts_quiz_id` (`quiz_id`)

### 3.5 `quiz_responses`
Student submitted answers for each question during an attempt.
* `response_id` VARCHAR(36) NOT NULL PRIMARY KEY
* `attempt_id` VARCHAR(36) NOT NULL FK -> `quiz_attempts(attempt_id)` ON DELETE CASCADE
* `question_id` VARCHAR(36) NOT NULL FK -> `quiz_questions(question_id)` ON DELETE CASCADE
* `selected_option_id` VARCHAR(36) NULL (For MCQ)
* `subjective_text` TEXT NULL (For SHORT_ANSWER)
* `is_correct` BOOLEAN NOT NULL DEFAULT FALSE (Evaluated server-side)
* `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
* `updated_at` DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6)
* **Indexes**:
  - `idx_quiz_responses_attempt_id` (`attempt_id`)
  - `idx_quiz_responses_question_id` (`question_id`)
