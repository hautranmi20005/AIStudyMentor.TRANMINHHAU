ALTER TABLE practice_quizzes
    ADD COLUMN topic VARCHAR(100) NULL,
    ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT 'SUBJECT_TOPIC',
    ADD COLUMN source_question_id VARCHAR(36) NULL;

ALTER TABLE quiz_questions
    ADD COLUMN topic VARCHAR(100) NULL,
    ADD COLUMN accepted_answers TEXT NULL;

CREATE INDEX idx_quiz_questions_topic ON quiz_questions (topic);
