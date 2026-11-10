package com.aistudymentor.data.repository

import androidx.room.withTransaction
import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.aistudymentor.data.local.OfflineDao
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OfflineRepository(
    private val api: ApiService,
    private val database: StudyMentorDatabase,
    private val dao: OfflineDao,
    private val tokenStorage: SecureTokenStorage,
    private val gson: Gson = Gson()
) {
    fun questionHistory(): Flow<List<CachedQuestionEntity>> =
        tokenStorage.userId()?.let(dao::observeQuestions) ?: flowOf(emptyList())

    fun bookmarks(): Flow<List<CachedBookmarkEntity>> =
        tokenStorage.userId()?.let(dao::observeBookmarks) ?: flowOf(emptyList())

    fun quizResults(): Flow<List<CachedQuizResultEntity>> =
        tokenStorage.userId()?.let(dao::observeQuizResults) ?: flowOf(emptyList())

    suspend fun cacheQuestion(response: QuestionResponseDto) {
        val accountId = tokenStorage.userId() ?: return
        val questionId = response.questionId ?: return
        dao.insertQuestion(
            CachedQuestionEntity(
                accountId = accountId,
                questionId = questionId,
                subject = response.subject.orEmpty(),
                questionSnippet = response.questionText.orEmpty().take(240),
                hasImage = !response.imageStoragePaths.isNullOrEmpty(),
                isBookmarked = response.isBookmarked == true,
                status = response.status.orEmpty(),
                createdAt = response.createdAt
            )
        )
    }

    /**
    * Pulls all pages before mutating Room, then applies this account's snapshots in one
    * database transaction. An interrupted or failed network sync leaves the last cache intact.
     */
    suspend fun syncFromRemote() {
        val accountId = tokenStorage.userId() ?: return
        val accessToken = tokenStorage.accessToken() ?: return
        val bearer = "Bearer $accessToken"
        val questions = fetchAllPages { page -> api.listQuestions(bearer, page, PAGE_SIZE) }
            .mapNotNull { it.toQuestionEntity(accountId) }
        val bookmarks = fetchAllPages { page -> api.listBookmarkedQuestions(bearer, page, PAGE_SIZE) }
            .mapNotNull { it.toBookmarkEntity(accountId) }
        val results = monitorRetrofitCall("offline.quiz_results.sync") {
            api.listCompletedQuizAttempts(bearer).requireBody()
        }
            .mapNotNull { result ->
                val attemptId = result.attemptId ?: return@mapNotNull null
                val quizId = result.quizId ?: return@mapNotNull null
                CachedQuizResultEntity(
                    accountId = accountId,
                    attemptId = attemptId,
                    quizId = quizId,
                    subject = result.subject.orEmpty(),
                    score = result.score ?: 0,
                    totalQuestions = result.totalQuestions ?: 0,
                    correctCount = result.correctCount ?: 0,
                    submittedAt = result.submittedAt,
                    detailsJson = gson.toJson(result.results.orEmpty())
                )
            }

        database.withTransaction {
            dao.insertQuestions(questions)
            dao.clearBookmarks(accountId)
            dao.insertBookmarks(bookmarks)
            dao.clearQuizResults(accountId)
            dao.insertQuizResults(results)
        }
    }

    private suspend fun fetchAllPages(
        requestPage: suspend (Int) -> retrofit2.Response<com.aistudymentor.data.remote.PageResponseDto<QuestionSummaryDto>>
    ): List<QuestionSummaryDto> {
        val items = mutableListOf<QuestionSummaryDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = monitorRetrofitCall("offline.question_history.sync") {
                requestPage(page).requireBody()
            }
            val content = response.content.orEmpty()
            items += content
            val isLast = response.last ?: (content.size < PAGE_SIZE)
            if (isLast || content.isEmpty()) return items
            page++
        }
        throw ApiFailure(null, "SYNC_PAGE_LIMIT", "History is too large to synchronize in one pass.", retryable = true)
    }

    private fun QuestionSummaryDto.toQuestionEntity(accountId: String): CachedQuestionEntity? {
        val id = questionId ?: return null
        return CachedQuestionEntity(
            accountId = accountId,
            questionId = id,
            subject = subject.orEmpty(),
            questionSnippet = questionSnippet.orEmpty(),
            hasImage = hasImage == true,
            isBookmarked = isBookmarked == true,
            status = status.orEmpty(),
            createdAt = createdAt
        )
    }

    private fun QuestionSummaryDto.toBookmarkEntity(accountId: String): CachedBookmarkEntity? {
        val id = questionId ?: return null
        return CachedBookmarkEntity(
            accountId = accountId,
            questionId = id,
            subject = subject.orEmpty(),
            questionSnippet = questionSnippet.orEmpty(),
            hasImage = hasImage == true,
            status = status.orEmpty(),
            createdAt = createdAt
        )
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 1000
    }
}
