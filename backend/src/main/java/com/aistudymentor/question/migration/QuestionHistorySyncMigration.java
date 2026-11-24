package com.aistudymentor.question.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public abstract class QuestionHistorySyncMigration extends BaseJavaMigration {

    @Override
    public final void migrate(Context context) throws Exception {
        String database = context.getConnection().getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE INDEX idx_questions_history_subject
                    ON questions (profile_id, subject, created_at)
                    """);
            statement.execute("""
                    CREATE INDEX idx_questions_history_status
                    ON questions (profile_id, status, created_at)
                    """);
            createSearchIndex(statement, database);
            createDeletionsTable(statement, database);
            statement.execute("""
                    CREATE INDEX idx_question_sync_deletions_profile_time
                    ON question_sync_deletions (profile_id, deleted_at)
                    """);
        }
    }

    private void createSearchIndex(Statement statement, String database) throws SQLException {
        if (database.contains("mysql")) {
            statement.execute("ALTER TABLE questions ADD FULLTEXT INDEX idx_questions_history_text (subject, question_text)");
        } else if (database.contains("postgresql")) {
            statement.execute("""
                    CREATE INDEX idx_questions_history_text ON questions USING GIN
                    (to_tsvector('simple', coalesce(subject, '') || ' ' || coalesce(question_text, '')))
                    """);
        } else {
            statement.execute("""
                    CREATE INDEX idx_questions_history_text
                    ON questions (profile_id, subject, created_at)
                    """);
        }
    }

    private void createDeletionsTable(Statement statement, String database) throws SQLException {
        String timestampType = database.contains("mysql") ? "DATETIME(6)" : "TIMESTAMP(6)";
        statement.execute("""
                CREATE TABLE question_sync_deletions (
                    deletion_id VARCHAR(36) NOT NULL,
                    profile_id VARCHAR(36) NOT NULL,
                    question_id VARCHAR(36) NOT NULL,
                    deleted_at %s NOT NULL,
                    CONSTRAINT pk_question_sync_deletions PRIMARY KEY (deletion_id)
                )
                """.formatted(timestampType));
    }
}
