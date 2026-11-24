package com.aistudymentor.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.security.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class OfflineRepositoryAccountFlowTest {
    private lateinit var database: StudyMentorDatabase
    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var repository: OfflineRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyMentorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tokenStorage = SecureTokenStorage(context)
        val api = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, _, _ -> null } as ApiService
        repository = OfflineRepository(api, database, database.offlineDao(), tokenStorage)
    }

    @After
    fun tearDown() {
        tokenStorage.clear()
        database.close()
    }

    @Test
    fun historyFlowIsScopedToTheCurrentAccount() = runBlocking {
        val dao = database.offlineDao()
        dao.insertQuestion(question("account-a", "question-a"))
        dao.insertQuestion(question("account-b", "question-b"))

        assertEquals(emptyList<CachedQuestionEntity>(), repository.questionHistory().first())

        tokenStorage.saveSession("token-a", "refresh-a", "account-a")
        val accountA = repository.questionHistory().first()
        assertEquals("account-a", accountA.single().accountId)
        assertEquals("question-a", accountA.single().questionId)

        tokenStorage.saveSession("token-b", "refresh-b", "account-b")
        val accountB = repository.questionHistory().first()
        assertEquals("account-b", accountB.single().accountId)
        assertEquals("question-b", accountB.single().questionId)

        tokenStorage.clear()
        assertEquals(emptyList<CachedQuestionEntity>(), repository.questionHistory().first())
    }

    @Test
    fun longLivedHistoryFlowTracksSignInAccountChanges() = runBlocking {
        val dao = database.offlineDao()
        dao.insertQuestion(question("account-a", "question-a"))
        dao.insertQuestion(question("account-b", "question-b"))

        val emissions = Channel<List<CachedQuestionEntity>>(Channel.UNLIMITED)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val collector = scope.launch {
            repository.questionHistory().collect { emissions.send(it) }
        }

        suspend fun awaitAccount(accountId: String): CachedQuestionEntity =
            withTimeout(5000) {
                emissions.receiveAsFlow()
                    .first { history -> history.any { it.accountId == accountId } }
                    .first { it.accountId == accountId }
            }

        try {
            assertEquals(emptyList<CachedQuestionEntity>(), withTimeout(5000) { emissions.receive() })

            tokenStorage.saveSession("token-a", "refresh-a", "account-a")
            assertEquals("question-a", awaitAccount("account-a").questionId)

            tokenStorage.saveSession("token-b", "refresh-b", "account-b")
            assertEquals("question-b", awaitAccount("account-b").questionId)

            tokenStorage.clear()
            assertEquals(emptyList<CachedQuestionEntity>(), withTimeout(5000) { emissions.receive() })
        } finally {
            collector.cancel()
            scope.cancel()
        }
    }

    private fun question(accountId: String, questionId: String) = CachedQuestionEntity(
        accountId = accountId,
        questionId = questionId,
        subject = "Mathematics",
        questionSnippet = "Solve an equation",
        hasImage = false,
        imageUrl = null,
        isBookmarked = false,
        status = "ANSWERED",
        createdAt = "2026-11-14T21:00:00",
        questionText = "Solve an equation",
        answerJson = null
    )
}
