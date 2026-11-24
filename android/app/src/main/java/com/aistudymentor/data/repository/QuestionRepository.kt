package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.AiQuotaResponseDto
import com.aistudymentor.data.remote.QuestionCreateRequestDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall

interface QuestionDataSource {
    suspend fun submitQuestion(subject: String, questionText: String): QuestionResponseDto
    suspend fun dailyQuota(): AiQuotaResponseDto? = null
}

class QuestionRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage,
    private val offlineRepository: OfflineRepository
) : QuestionDataSource {
    suspend fun getQuestion(questionId: String): QuestionResponseDto {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to open this question.", retryable = false)
        val response = monitorRetrofitCall("question.detail") {
            api.getQuestion("Bearer $token", questionId).requireBody()
        }
        offlineRepository.cacheQuestion(response)
        return response
    }

    suspend fun setBookmark(questionId: String, bookmarked: Boolean) {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to update this bookmark.", retryable = false)
        val bearer = "Bearer $token"
        val current = monitorRetrofitCall("question.bookmark.current") {
            api.getQuestion(bearer, questionId).requireBody()
        }
        val result = monitorRetrofitCall("question.bookmark.set") {
            if (bookmarked) api.addBookmark(bearer, questionId).requireBody()
            else api.removeBookmark(bearer, questionId).requireBody()
        }
        if (result.isBookmarked != bookmarked) {
            throw ApiFailure(
                null, "BOOKMARK_STATE_MISMATCH",
                "The server did not apply the requested bookmark state.", retryable = true
            )
        }
        offlineRepository.cacheQuestion(current.copy(isBookmarked = bookmarked))
    }

    override suspend fun submitQuestion(subject: String, questionText: String): QuestionResponseDto {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in before asking a question.", retryable = false)
        val response = monitorRetrofitCall("ai.submit_question") {
            api.submitQuestion(
                token = "Bearer $token",
                request = QuestionCreateRequestDto(subject = subject, questionText = questionText)
            ).requireBody()
        }
        offlineRepository.cacheQuestion(response)
        return response
    }

    override suspend fun dailyQuota(): AiQuotaResponseDto {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to view your AI quota.", retryable = false)
        return monitorRetrofitCall("ai.daily_quota") {
            api.getAiQuota("Bearer $token").requireBody()
        }
    }
}