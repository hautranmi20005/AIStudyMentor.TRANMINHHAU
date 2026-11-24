CREATE INDEX idx_questions_profile_created_subject
    ON questions (profile_id, created_at, subject);

CREATE INDEX idx_quiz_attempts_profile_end_time
    ON quiz_attempts (profile_id, end_time);

CREATE INDEX idx_quiz_responses_attempt_question_correct
    ON quiz_responses (attempt_id, question_id, is_correct);

CREATE INDEX idx_practice_quizzes_profile_subject
    ON practice_quizzes (profile_id, subject);

CREATE INDEX idx_quiz_questions_quiz_topic
    ON quiz_questions (quiz_id, topic);
