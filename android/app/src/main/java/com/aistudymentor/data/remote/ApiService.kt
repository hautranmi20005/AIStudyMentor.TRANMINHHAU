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
    val structuredExplanation: StructuredExplanationDto?,
    val isDemo: Boolean = false
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
data class AiQuotaResponseDto(
    val remainingCallsToday: Long,
    val remainingTokensToday: Long
)

data class QuizResultItemDto(
    val questionId: String?,
    val isCorrect: Boolean?,
    val correctAnswer: String?,
    val explanation: String?,
    val topic: String? = null
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

data class StudentProfileDto(
    val profileId: String?,
    val userId: String?,
    val educationLevel: String?,
    val learningStyle: String?
) {
    val onboardingCompleted: Boolean
        get() = !educationLevel.isNullOrBlank() &&
            !learningStyle.isNullOrBlank()
}

data class StudentProfileUpdateRequestDto(
    val educationLevel: String? = null,
    val learningStyle: String? = null
)

data class NotificationPreferencesDto(
    val bookmarkReviewEnabled: Boolean,
    val repeatedMistakeEnabled: Boolean,
    val weeklySummaryEnabled: Boolean,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val timezone: String = "UTC"
)

data class AccountReauthenticationRequestDto(val password: String, val code: String)
data class LogoutRequestDto(val refreshToken: String)
data class PasswordRecoveryRequestDto(val email: String)
data class PasswordResetRequestDto(val token: String, val newPassword: String)
data class QuestionBookmarkResponseDto(val questionId: String?, val isBookmarked: Boolean)

data class PracticeQuizDto(
    val quizId: String?,
    val subject: String?,
    val status: String?,
    val questions: List<PracticeQuizQuestionDto>?,
    val createdAt: String?,
    val topic: String? = null
)

data class PracticeQuizQuestionDto(
    val questionId: String?,
    val type: String?,
    val prompt: String?,
    val options: List<QuizOptionDto>?
)

data class QuizOptionDto(
    val optionId: String?,
    val optionText: String?,
    val optionOrder: Int?
)

data class QuizEditQuestionDto(
    val prompt: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)
data class QuizEditResponseDto(
    val quizId: String?,
    val subject: String?,
    val topic: String?,
    val questions: List<QuizEditQuestionDto>?
)
data class QuizGenerateRequestDto(
    val subject: String,
    val numQuestions: Int = 5,
    val topic: String? = null
)
data class QuizUpdateRequestDto(
    val subject: String,
    val topic: String? = null,
    val questions: List<QuizEditQuestionDto>? = null
)
data class QuizAttemptStartDto(val attemptId: String?, val quizId: String?, val startTime: String?)
data class QuizAnswerDto(
    val questionId: String,
    val selectedOptionId: String? = null,
    val subjectiveText: String? = null
)
data class QuizSubmitRequestDto(val responses: List<QuizAnswerDto>)
data class QuizSubmitResponseDto(
    val attemptId: String?,
    val quizId: String? = null,
    val score: Int?,
    val totalQuestions: Int?,
    val correctCount: Int?,
    val results: List<QuizResultItemDto>?,
    val submittedAt: String?
)

interface ApiService {
    @GET("ai/quota")
    suspend fun getAiQuota(@Header("Authorization") token: String): Response<AiQuotaResponseDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/mfa/verify")
    suspend fun verifyMfa(@Body request: MfaVerifyRequestDto): Response<MfaVerifyResponseDto>

    @POST("auth/password/forgot")
    suspend fun requestPasswordReset(@Body request: PasswordRecoveryRequestDto): Response<Void>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body request: PasswordResetRequestDto): Response<Void>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Void>

    @GET("profile/me")
    suspend fun getMyProfile(@Header("Authorization") token: String): Response<StudentProfileDto>

    @PATCH("profile/me")
    suspend fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body request: StudentProfileUpdateRequestDto
    ): Response<StudentProfileDto>

    @POST("account/logout-all")
    suspend fun logoutAllDevices(
        @Header("Authorization") token: String,
        @Body request: AccountReauthenticationRequestDto
    ): Response<Void>

    @HTTP(method = "DELETE", path = "account", hasBody = true)
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body request: AccountReauthenticationRequestDto
    ): Response<Void>

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

    @POST("questions/{id}/bookmark")
    suspend fun addBookmark(
        @Header("Authorization") token: String,
        @Path("id") questionId: String
    ): Response<QuestionBookmarkResponseDto>

    @DELETE("questions/{id}/bookmark")
    suspend fun removeBookmark(
        @Header("Authorization") token: String,
        @Path("id") questionId: String
    ): Response<QuestionBookmarkResponseDto>

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

    @GET("quizzes")
    suspend fun listQuizzes(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponseDto<PracticeQuizDto>>

    @GET("quizzes/{id}")
    suspend fun getQuiz(
        @Header("Authorization") token: String,
        @Path("id") quizId: String
    ): Response<PracticeQuizDto>

    @GET("quizzes/{id}/edit")
    suspend fun getQuizForEdit(
        @Header("Authorization") token: String,
        @Path("id") quizId: String
    ): Response<QuizEditResponseDto>

    @POST("quizzes")
    suspend fun generateQuiz(
        @Header("Authorization") token: String,
        @Body request: QuizGenerateRequestDto
    ): Response<PracticeQuizDto>

    @PUT("quizzes/{id}")
    suspend fun updateQuiz(
        @Header("Authorization") token: String,
        @Path("id") quizId: String,
        @Body request: QuizUpdateRequestDto
    ): Response<PracticeQuizDto>

    @DELETE("quizzes/{id}")
    suspend fun deleteQuiz(
        @Header("Authorization") token: String,
        @Path("id") quizId: String
    ): Response<Void>

    @POST("quizzes/{id}/attempts")
    suspend fun startQuizAttempt(
        @Header("Authorization") token: String,
        @Path("id") quizId: String
    ): Response<QuizAttemptStartDto>

    @POST("quizzes/{id}/attempts/{attemptId}/submit")
    suspend fun submitQuizAttempt(
        @Header("Authorization") token: String,
        @Path("id") quizId: String,
        @Path("attemptId") attemptId: String,
        @Body request: QuizSubmitRequestDto
    ): Response<QuizSubmitResponseDto>

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

    @GET("notifications/preferences")
    suspend fun getNotificationPreferences(
        @Header("Authorization") token: String
    ): Response<NotificationPreferencesDto>

    @PUT("notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Header("Authorization") token: String,
        @Body request: NotificationPreferencesDto
    ): Response<NotificationPreferencesDto>
}
