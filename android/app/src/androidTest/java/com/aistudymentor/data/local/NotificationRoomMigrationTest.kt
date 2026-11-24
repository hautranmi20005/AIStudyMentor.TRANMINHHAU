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
class NotificationRoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyMentorDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun versionFourMigrationAddsLocalNotificationStateAndBookmarkViewTime() {
        val databaseName = "notification-migration-test"
        helper.createDatabase(databaseName, 4).close()

        helper.runMigrationsAndValidate(
            databaseName,
            5,
            true,
            StudyMentorDatabase.MIGRATION_4_5
        ).use { database ->
            database.execSQL(
                "INSERT INTO notification_preferences " +
                    "(accountId, bookmarkReviewEnabled, repeatedMistakeEnabled, weeklySummaryEnabled, " +
                    "quietHoursStart, quietHoursEnd, timezone, pendingUpload) " +
                    "VALUES ('account-a', 0, 1, 1, NULL, NULL, 'UTC', 1)"
            )
            database.query(
                "SELECT bookmarkReviewEnabled, pendingUpload FROM notification_preferences " +
                    "WHERE accountId='account-a'"
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals(1, cursor.getInt(1))
            }
            database.query("PRAGMA table_info(cached_bookmarks)").use { cursor ->
                val columnNames = buildList {
                    while (cursor.moveToNext()) add(cursor.getString(1))
                }
                assertEquals(true, "lastViewedAt" in columnNames)
            }
        }
    }
}
