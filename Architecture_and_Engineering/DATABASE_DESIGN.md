# Database Design v1.1

## 1. Core Entities
- `users`: user_id (PK), email, password_hash, dob, account_status.
- `mfa_factors`: factor_id (PK), user_id (FK), type, secret, is_verified.
- `guardian_consents`: consent_id (PK), user_id (FK), guardian_email, status, verified_at.
- `student_profiles`: profile_id (PK), user_id (FK), education_level.
- `questions` & `question_attachments`.
- `answers` & `ai_generation_logs`.

## 2. Extended MVP Entities
- `practice_quizzes`: quiz_id (PK), profile_id (FK), subject, status, created_at.
- `quiz_questions`: question_id (PK), quiz_id (FK), type, prompt, correct_answer_hash (server-side only).
- `quiz_options`: option_id (PK), question_id (FK), option_text.
- `quiz_attempts`: attempt_id (PK), quiz_id (FK), start_time, end_time, score.
- `quiz_responses`: response_id (PK), attempt_id (FK), question_id (FK), selected_option, subjective_text, is_correct.
