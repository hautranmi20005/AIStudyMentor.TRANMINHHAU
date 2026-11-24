package com.aistudymentor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedQuestionEntity::class,
        CachedBookmarkEntity::class,
        CachedQuizResultEntity::class,
        PendingBookmarkChangeEntity::class,
        OfflineSyncStateEntity::class,
        CachedDashboardMetricsEntity::class,
        NotificationPreferencesEntity::class,
        LocalNotificationDeliveryEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class StudyMentorDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        @Volatile
        private var instance: StudyMentorDatabase? = null

        fun getInstance(context: Context): StudyMentorDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StudyMentorDatabase::class.java,
                "study-mentor-offline.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_questions ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE cached_bookmarks ADD COLUMN imageUrl TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_questions RENAME TO cached_questions_v2")
                db.execSQL(
                    "CREATE TABLE cached_questions (" +
                        "accountId TEXT NOT NULL, questionId TEXT NOT NULL, subject TEXT NOT NULL, " +
                        "questionSnippet TEXT NOT NULL, hasImage INTEGER NOT NULL, imageUrl TEXT, " +
                        "isBookmarked INTEGER NOT NULL, status TEXT NOT NULL, createdAt TEXT, " +
                        "questionText TEXT NOT NULL, answerJson TEXT, PRIMARY KEY(accountId, questionId))"
                )
                db.execSQL(
                    "INSERT INTO cached_questions " +
                        "(accountId, questionId, subject, questionSnippet, hasImage, imageUrl, isBookmarked, " +
                        "status, createdAt, questionText, answerJson) " +
                        "SELECT accountId, questionId, subject, questionSnippet, hasImage, imageUrl, isBookmarked, " +
                        "status, createdAt, questionSnippet, NULL FROM cached_questions_v2"
                )
                db.execSQL("DROP TABLE cached_questions_v2")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pending_bookmark_changes (" +
                        "accountId TEXT NOT NULL, questionId TEXT NOT NULL, isBookmarked INTEGER NOT NULL, " +
                        "PRIMARY KEY(accountId, questionId))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS offline_sync_state (" +
                        "accountId TEXT NOT NULL PRIMARY KEY, cursor TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cached_dashboard_metrics (" +
                        "accountId TEXT NOT NULL, `range` TEXT NOT NULL, timezone TEXT NOT NULL, " +
                        "metricsJson TEXT NOT NULL, cachedAt TEXT NOT NULL, " +
                        "PRIMARY KEY(accountId, `range`, timezone))"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_bookmarks ADD COLUMN lastViewedAt TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notification_preferences (" +
                        "accountId TEXT NOT NULL PRIMARY KEY, bookmarkReviewEnabled INTEGER NOT NULL, " +
                        "repeatedMistakeEnabled INTEGER NOT NULL, weeklySummaryEnabled INTEGER NOT NULL, " +
                        "quietHoursStart TEXT, quietHoursEnd TEXT, timezone TEXT NOT NULL, " +
                        "pendingUpload INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS local_notification_deliveries (" +
                        "accountId TEXT NOT NULL, type TEXT NOT NULL, periodKey TEXT NOT NULL, " +
                        "createdAt TEXT NOT NULL, PRIMARY KEY(accountId, type, periodKey))"
                )
            }
        }
    }
}
