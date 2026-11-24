ALTER TABLE question_sync_deletions
    ADD CONSTRAINT fk_question_sync_deletions_profile
        FOREIGN KEY (profile_id) REFERENCES student_profiles (profile_id) ON DELETE CASCADE;
