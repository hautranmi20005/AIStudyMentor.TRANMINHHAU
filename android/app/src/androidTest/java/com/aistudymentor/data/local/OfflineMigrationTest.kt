package com.aistudymentor.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyMentorDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrationFromVersionTwoPreservesHistoryAndAddsSyncFields() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                "INSERT INTO cached_questions " +
                    "(accountId, questionId, subject, questionSnippet, hasImage, imageUrl, isBookmarked, status, createdAt) " +
                    "VALUES ('account-a', 'question-1', 'Math', 'Solve x', 0, NULL, 0, 'ANSWERED', '2026-01-01')"
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, StudyMentorDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT questionText, answerJson FROM cached_questions WHERE questionId = 'question-1'").use {
                assertEquals(true, it.moveToFirst())
                assertEquals("Solve x", it.getString(0))
                assertEquals(true, it.isNull(1))
            }
            db.query("SELECT COUNT(*) FROM pending_bookmark_changes").use {
                assertEquals(true, it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "offline-migration-test"
    }
}
