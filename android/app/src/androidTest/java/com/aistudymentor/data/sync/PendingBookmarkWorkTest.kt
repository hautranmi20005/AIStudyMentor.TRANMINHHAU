package com.aistudymentor.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.aistudymentor.data.local.OfflineDao
import com.aistudymentor.data.local.PendingBookmarkChangeEntity
import com.aistudymentor.data.local.StudyMentorDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PendingBookmarkWorkTest {
    private lateinit var context: Context
    private lateinit var database: StudyMentorDatabase
    private lateinit var dao: OfflineDao
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(SYNC_TAG).result.get(10, TimeUnit.SECONDS)
        database = Room.inMemoryDatabaseBuilder(context, StudyMentorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.offlineDao()
    }

    @After
    fun tearDown() {
        workManager.cancelAllWorkByTag(SYNC_TAG).result.get(10, TimeUnit.SECONDS)
        database.close()
    }

    @Test
    fun pendingBookmarkHasNetworkConstrainedRetryWorkQueued() {
        kotlinx.coroutines.runBlocking {
            dao.savePendingBookmarkChange(PendingBookmarkChangeEntity("account-a", "question-1", true))
        }
        OfflineSyncCoordinator(context).enqueueSync()

        val work = workManager.getWorkInfosByTag(SYNC_TAG).get(10, TimeUnit.SECONDS)
        assertTrue(work.isNotEmpty())
        val syncWork = work.first { it.tags.contains(SYNC_TAG) }
        assertEquals(NetworkType.CONNECTED, syncWork.constraints.requiredNetworkType)
    }

    private companion object {
        const val SYNC_TAG = "offline-history"
    }
}
