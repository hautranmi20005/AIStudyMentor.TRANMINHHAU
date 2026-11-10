package com.aistudymentor.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class LoginRequestDto(val email: String, val password: String)
data class LoginResponseDto(
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String?,
    val userId: String?,
    val mfaRequired: Boolean?,
    val mfaSessionToken: String?,
    val email: String?,
    val accountStatus: String?,
    val expiresIn: Long?
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerializedName("dob") val dateOfBirth: String,
    val educationLevel: String,
    val guardianEmail: String?
)
data class RegisterResponseDto(
    val userId: String?,
    val email: String?,
    val accountStatus: String?,
    val consentStatus: String?,
    val guardianConsentRequired: Boolean?,
    val mfaRequired: Boolean?,
    val message: String?
)

data class MfaVerifyRequestDto(val mfaSessionToken: String, val code: String, val type: String = "TOTP")
data class MfaVerifyResponseDto(
    val userId: String?,
    val email: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String?,
    val expiresIn: Long?
)

data class QuestionCreateRequestDto(
    val subject: String,
    val questionText: String,
    val imageStoragePath: String? = null
)
data class QuestionResponseDto(
    val questionId: String?,
    val subject: String?,
    val questionText: String?,
    val status: String?,
    val isBookmarked: Boolean?,
    val imageStoragePaths: List<String>?,
    val answer: AnswerResponseDto?,
    val createdAt: String?
)
data class QuestionSummaryDto(
    val questionId: String?,
    val subject: String?,
    val questionSnippet: String?,
    val hasImage: Boolean?,
    val isBookmarked: Boolean?,
    val status: String?,
    val createdAt: String?
)
data class PageResponseDto<T>(
    val content: List<T>?,
    val last: Boolean?,
    val totalPages: Int?
)
data class AnswerResponseDto(
    val answerId: String?,
    val isAlternative: Boolean?,
    val structuredExplanation: StructuredExplanationDto?
)
data class StructuredExplanationDto(
    val summary: String?,
    val steps: List<ExplanationStepDto>?,
    val finalAnswer: String?,
    val keyTakeaways: List<String>?
)
data class ExplanationStepDto(
    val stepNumber: Int,
    val title: String?,
    val explanation: String?,
    val formula: String?
)

data class GuardianConsentResponseDto(
    val consentId: String?,
    val userId: String?,
    val status: String?,
    val guardianEmail: String?,
    val verifiedAt: String?,
    val canAccessCoreFeatures: Boolean?
)

data class DashboardMetricsDto(
    val totalQuestionsAsked: Long?,
    val totalQuizzesCompleted: Long?,
    val overallAccuracyRate: Double?,
    val subjectBreakdown: List<SubjectBreakdownDto>?
)

data class QuizResultItemDto(
    val questionId: String?,
    val isCorrect: Boolean?,
    val correctAnswer: String?,
    val explanation: String?
)

data class QuizAttemptHistoryDto(
    val attemptId: String?,
    val quizId: String?,
    val subject: String?,
    val score: Int?,
    val totalQuestions: Int?,
    val correctCount: Int?,
    val submittedAt: String?,
    val results: List<QuizResultItemDto>?
)

data class SubjectBreakdownDto(
    val subject: String?,
    val questionsCount: Long?,
    val quizCount: Long?,
    val accuracyRate: Double?
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/mfa/verify")
    suspend fun verifyMfa(@Body request: MfaVerifyRequestDto): Response<MfaVerifyResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("questions")
    suspend fun submitQuestion(
        @Header("Authorization") token: String,
        @Body request: QuestionCreateRequestDto
    ): Response<QuestionResponseDto>

    @GET("questions/{id}")
    suspend fun getQuestion(
        @Header("Authorization") token: String,
        @Path("id") questionId: String
    ): Response<QuestionResponseDto>

    @GET("questions")
    suspend fun listQuestions(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PageResponseDto<QuestionSummaryDto>>

    @GET("questions/bookmarks")
    suspend fun listBookmarkedQuestions(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PageResponseDto<QuestionSummaryDto>>

    @GET("quizzes/attempts")
    suspend fun listCompletedQuizAttempts(
        @Header("Authorization") token: String
    ): Response<List<QuizAttemptHistoryDto>>

    @GET("guardian-consent/status")
    suspend fun getGuardianConsentStatus(
        @Header("Authorization") token: String
    ): Response<GuardianConsentResponseDto>

    @POST("guardian-consent/resend")
    suspend fun resendGuardianConsent(
        @Header("Authorization") token: String
    ): Response<Void>

    @GET("dashboard/metrics")
    suspend fun getDashboardMetrics(
        @Header("Authorization") token: String
    ): Response<DashboardMetricsDto>
}
