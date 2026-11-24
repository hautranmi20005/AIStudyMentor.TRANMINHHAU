package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.QuizAttemptStartDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizEditResponseDto
import com.aistudymentor.data.remote.QuizSubmitRequestDto
import com.aistudymentor.data.remote.QuizSubmitResponseDto
import com.aistudymentor.data.remote.QuizUpdateRequestDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall

interface QuizRepository {
    suspend fun listQuizzes(): List<PracticeQuizDto>
    suspend fun listCompletedAttempts(): List<QuizAttemptHistoryDto>
    suspend fun listRecentQuestions(): List<QuestionSummaryDto>
    suspend fun getQuiz(quizId: String): PracticeQuizDto
    suspend fun getQuizForEdit(quizId: String): QuizEditResponseDto
    suspend fun generateQuiz(request: QuizGenerateRequestDto): PracticeQuizDto
    suspend fun updateQuiz(quizId: String, request: QuizUpdateRequestDto): PracticeQuizDto
    suspend fun deleteQuiz(quizId: String)
    suspend fun startAttempt(quizId: String): QuizAttemptStartDto
    suspend fun submitAttempt(
        quizId: String,
        attemptId: String,
        request: QuizSubmitRequestDto
    ): QuizSubmitResponseDto
    suspend fun getAttemptReview(attemptId: String): QuizSubmitResponseDto
}

class ApiQuizRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage
) : QuizRepository {

    override suspend fun listQuizzes(): List<PracticeQuizDto> =
        monitorRetrofitCall("quiz.list") {
            api.listQuizzes(requireBearer(), size = 50).requireBody().content.orEmpty()
        }

    override suspend fun listCompletedAttempts(): List<QuizAttemptHistoryDto> =
        monitorRetrofitCall("quiz.attempts.list") {
            api.listCompletedQuizAttempts(requireBearer()).requireBody()
        }

    override suspend fun listRecentQuestions(): List<QuestionSummaryDto> =
        monitorRetrofitCall("quiz.recent_questions") {
            api.listQuestions(requireBearer(), size = 10).requireBody().content.orEmpty()
        }

    override suspend fun getQuiz(quizId: String): PracticeQuizDto =
        monitorRetrofitCall("quiz.get") {
            api.getQuiz(requireBearer(), quizId).requireBody()
        }

    override suspend fun getQuizForEdit(quizId: String): QuizEditResponseDto =
        monitorRetrofitCall("quiz.edit.get") {
            api.getQuizForEdit(requireBearer(), quizId).requireBody()
        }

    override suspend fun generateQuiz(request: QuizGenerateRequestDto): PracticeQuizDto =
        monitorRetrofitCall("quiz.generate") {
            api.generateQuiz(requireBearer(), request).requireBody()
        }

    override suspend fun updateQuiz(quizId: String, request: QuizUpdateRequestDto): PracticeQuizDto =
        monitorRetrofitCall("quiz.update") {
            api.updateQuiz(requireBearer(), quizId, request).requireBody()
        }

    override suspend fun deleteQuiz(quizId: String) {
        monitorRetrofitCall("quiz.delete") {
            api.deleteQuiz(requireBearer(), quizId).requireSuccess()
        }
    }

    override suspend fun startAttempt(quizId: String): QuizAttemptStartDto =
        monitorRetrofitCall("quiz.attempt.start") {
            api.startQuizAttempt(requireBearer(), quizId).requireBody()
        }

    override suspend fun submitAttempt(
        quizId: String,
        attemptId: String,
        request: QuizSubmitRequestDto
    ): QuizSubmitResponseDto =
        monitorRetrofitCall("quiz.attempt.submit") {
            api.submitQuizAttempt(requireBearer(), quizId, attemptId, request).requireBody()
        }

    override suspend fun getAttemptReview(attemptId: String): QuizSubmitResponseDto =
        listCompletedAttempts().firstOrNull { it.attemptId == attemptId }?.let { attempt ->
            QuizSubmitResponseDto(
                attemptId = attempt.attemptId,
                quizId = attempt.quizId,
                score = attempt.score,
                totalQuestions = attempt.totalQuestions,
                correctCount = attempt.correctCount,
                results = attempt.results,
                submittedAt = attempt.submittedAt
            )
        } ?: throw ApiFailure(
            404, "ATTEMPT_NOT_FOUND", "The completed quiz attempt was not found.", retryable = false
        )

    private fun requireBearer(): String {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to access quizzes.", retryable = false)
        return "Bearer $token"
    }
}
