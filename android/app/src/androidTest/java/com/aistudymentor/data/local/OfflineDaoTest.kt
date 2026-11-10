package com.aistudymentor.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineDaoTest {
    private lateinit var database: StudyMentorDatabase
    private lateinit var dao: OfflineDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            StudyMentorDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.offlineDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun cachesHistoryBookmarksAndQuizResultsPerAccount() = runBlocking {
        dao.insertQuestion(CachedQuestionEntity("account-a", "question-1", "Math", "Solve x", false, true, "ANSWERED", "2026-01-01"))
        dao.insertBookmarks(listOf(CachedBookmarkEntity("account-a", "question-1", "Math", "Solve x", false, "ANSWERED", "2026-01-01")))
        dao.insertQuizResult(CachedQuizResultEntity("account-a", "attempt-1", "quiz-1", "Math", 80, 5, 4, "2026-01-02", "[]"))

        assertEquals(1, dao.observeQuestions("account-a").first().size)
        assertEquals(1, dao.observeBookmarks("account-a").first().size)
        assertEquals(1, dao.observeQuizResults("account-a").first().size)
        assertEquals(0, dao.observeQuestions("account-b").first().size)
        assertEquals(0, dao.observeBookmarks("account-b").first().size)
        assertEquals(0, dao.observeQuizResults("account-b").first().size)
    }
}