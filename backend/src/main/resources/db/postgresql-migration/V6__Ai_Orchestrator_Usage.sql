ALTER TABLE ai_generation_logs
    ADD COLUMN outcome VARCHAR(50) NOT NULL DEFAULT 'SUCCESS';

ALTER TABLE quiz_questions
    ADD COLUMN explanation TEXT NULL;
