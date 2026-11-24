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
class DashboardRoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyMentorDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun version3MigratesToAccountScopedDashboardSnapshots() {
        val databaseName = "dashboard-migration-test"
        helper.createDatabase(databaseName, 3).close()

        val database = helper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            StudyMentorDatabase.MIGRATION_3_4
        )
        database.execSQL(
            "INSERT INTO cached_dashboard_metrics " +
                "(accountId, range, timezone, metricsJson, cachedAt) " +
                "VALUES ('account-a', '30d', 'UTC', '{}', '2026-01-01T00:00:00Z')"
        )
        database.query(
            "SELECT COUNT(*) FROM cached_dashboard_metrics WHERE accountId='account-a'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        database.close()
    }
}
