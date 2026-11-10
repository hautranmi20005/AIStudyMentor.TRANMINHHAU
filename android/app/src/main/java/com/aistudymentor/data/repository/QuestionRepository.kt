package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.QuestionCreateRequestDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall

class QuestionRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage,
    private val offlineRepository: OfflineRepository
) {
    suspend fun submitQuestion(subject: String, questionText: String): QuestionResponseDto {
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
}