package com.aistudymentor.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import androidx.room.Room
import androidx.test.espresso.Espresso
import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.AccountReauthenticationRequestDto
import com.aistudymentor.data.remote.AiQuotaResponseDto
import com.aistudymentor.data.remote.AnswerResponseDto
import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.remote.ExplanationStepDto
import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.remote.LogoutRequestDto
import com.aistudymentor.data.remote.LoginRequestDto
import com.aistudymentor.data.remote.LoginResponseDto
import com.aistudymentor.data.remote.MfaVerifyRequestDto
import com.aistudymentor.data.remote.MfaVerifyResponseDto
import com.aistudymentor.data.remote.NotificationPreferencesDto
import com.aistudymentor.data.remote.PageResponseDto
import com.aistudymentor.data.remote.PasswordRecoveryRequestDto
import com.aistudymentor.data.remote.PasswordResetRequestDto
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.QuestionBookmarkResponseDto
import com.aistudymentor.data.remote.QuestionCreateRequestDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.QuizAttemptStartDto
import com.aistudymentor.data.remote.QuizEditResponseDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizSubmitRequestDto
import com.aistudymentor.data.remote.QuizSubmitResponseDto
import com.aistudymentor.data.remote.QuizUpdateRequestDto
import com.aistudymentor.data.remote.RegisterRequestDto
import com.aistudymentor.data.remote.RegisterResponseDto
import com.aistudymentor.data.remote.StudentProfileDto
import com.aistudymentor.data.remote.StudentProfileUpdateRequestDto
import com.aistudymentor.data.remote.StructuredExplanationDto
import com.aistudymentor.data.remote.SubjectBreakdownDto
import com.aistudymentor.data.repository.AuthRepository
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.ui.ai.AnswerDetailScreen
import com.aistudymentor.ui.ai.AskAiScreen
import com.aistudymentor.ui.ai.AskAiViewModel
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.ui.auth.LoginScreen
import com.aistudymentor.ui.history.HistoryScreen
import com.aistudymentor.ui.history.OfflineLibraryViewModel
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class StudentFlowsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var fakeApi: FakeApiService
    private lateinit var database: StudyMentorDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenStorage = SecureTokenStorage(context)
        tokenStorage.clear()
        fakeApi = FakeApiService()
        database = Room.inMemoryDatabaseBuilder(context, StudyMentorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        tokenStorage.clear()
        database.close()
    }

    @Test
    fun loginScreenAuthenticatesAndSignalsGuardianRoutingState() {
        val viewModel = AuthViewModel(AuthRepository(fakeApi, tokenStorage))
        val authenticatedWithConsent = AtomicReference<Boolean?>(null)

        composeRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { authenticatedWithConsent.set(it) },
                onNavigateToRegister = {}
            )
        }

        composeRule.onNodeWithText("Email").performTextInput("student@example.test")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        Espresso.closeSoftKeyboard()
        composeRule.onAllNodesWithText("Sign in")[0].performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { authenticatedWithConsent.get() != null }

        assertFalse(authenticatedWithConsent.get() == true)
        assertEquals("access-token", tokenStorage.accessToken())
        assertEquals("refresh-token", tokenStorage.refreshToken())
    }

    @Test
    fun askAiScreenSubmitsQuestionAndNavigatesToExplanation() {
        tokenStorage.saveSession("access-token", "refresh-token", "student-id")
        val offlineRepository = OfflineRepository(fakeApi, database, database.offlineDao(), tokenStorage)
        val viewModel = AskAiViewModel(QuestionRepository(fakeApi, tokenStorage, offlineRepository))
        val navigated = AtomicReference(false)

        composeRule.setContent {
            AskAiScreen(viewModel = viewModel, onNavigateToAnswer = { navigated.set(true) })
        }

        composeRule.onNodeWithText("Ask a study question or paste problem text")
            .performTextInput("Solve x + 2 = 7")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Solve problem").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navigated.get() }

        assertEquals("Mathematics", fakeApi.lastQuestionRequest?.subject)
        assertEquals("Solve x + 2 = 7", fakeApi.lastQuestionRequest?.questionText)
        runBlocking {
            assertEquals(1, database.offlineDao().observeQuestions("student-id").first().size)
        }
    }

    @Test
    fun answerDetailScreenRendersSummaryStepsFormulaAndFinalAnswer() {
        val explanation = StructuredExplanationDto(
            summary = "Subtract two from both sides to isolate x.",
            steps = listOf(ExplanationStepDto(1, "Isolate x", "Subtract two from each side.", "x = 5")),
            finalAnswer = "x = 5",
            keyTakeaways = listOf("Use inverse operations.")
        )
        composeRule.setContent { AnswerDetailScreen(explanation = explanation, onBack = {}) }

        composeRule.onNodeWithText("Subtract two from both sides to isolate x.").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1: Isolate x").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Subtract two from each side.").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("x = 5").assertCountEquals(2)
        composeRule.onNodeWithText("Use inverse operations.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun historyAndBookmarkItemsNavigateToSavedQuestion() {
        tokenStorage.saveSession("access-token", "refresh-token", "student-id")
        val questionId = "saved-question"
        insertCachedAnswer(questionId)
        val libraryViewModel = createOfflineLibraryViewModel()
        val openedQuestionId = AtomicReference<String?>()
        composeRule.setContent {
            HistoryScreen(libraryViewModel) { openedQuestionId.set(it) }
        }
        composeRule.waitUntil(5_000) {
            libraryViewModel.uiState.value.questions.any { it.questionId == questionId } &&
                !libraryViewModel.uiState.value.isSyncing
        }

        composeRule.onNodeWithText("Saved history question").performClick()
        assertEquals(questionId, openedQuestionId.get())

        runBlocking {
            database.offlineDao().insertBookmarks(
                listOf(
                    CachedBookmarkEntity(
                        accountId = "student-id",
                        questionId = questionId,
                        subject = "Mathematics",
                        questionSnippet = "Saved bookmark item",
                        hasImage = false,
                        status = "ANSWERED",
                        createdAt = "2026-01-01T00:00:00"
                    )
                )
            )
        }
        composeRule.waitUntil(5_000) {
            libraryViewModel.uiState.value.bookmarks.any { it.questionId == questionId }
        }
        composeRule.onNodeWithText("Bookmarks").performClick()
        composeRule.onNodeWithText("Saved bookmark item").performClick()
        assertEquals(questionId, openedQuestionId.get())
    }

    @Test
    fun savedQuestionDetailRendersCachedAnswerAndBookmarkAction() {
        tokenStorage.saveSession("access-token", "refresh-token", "student-id")
        val questionId = "saved-question"
        insertCachedAnswer(questionId)
        val libraryViewModel = createOfflineLibraryViewModel()

        composeRule.setContent {
            AnswerDetailScreen(questionId, libraryViewModel, onBack = {})
        }
        composeRule.waitUntil(5_000) { libraryViewModel.uiState.value.detail?.questionId == questionId }
        composeRule.onNodeWithText("Solve x + 2 = 7").assertIsDisplayed()
        composeRule.onNodeWithText("A saved explanation.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Bookmark question").assertIsDisplayed()
    }

    private fun insertCachedAnswer(questionId: String) {
        val answer = AnswerResponseDto(
            "answer-id",
            false,
            StructuredExplanationDto(
                "A saved explanation.",
                listOf(ExplanationStepDto(1, "Solve", "Use the given equation.", "x = 5")),
                "x = 5",
                listOf("Check the result.")
            )
        )
        runBlocking {
            database.offlineDao().insertQuestion(
                CachedQuestionEntity(
                    accountId = "student-id",
                    questionId = questionId,
                    subject = "Mathematics",
                    questionSnippet = "Saved history question",
                    hasImage = false,
                    isBookmarked = false,
                    status = "ANSWERED",
                    createdAt = "2026-01-01T00:00:00",
                    questionText = "Solve x + 2 = 7",
                    answerJson = Gson().toJson(answer)
                )
            )
        }
    }

    private fun createOfflineLibraryViewModel(): OfflineLibraryViewModel {
        val offlineRepository = OfflineRepository(fakeApi, database, database.offlineDao(), tokenStorage)
        return OfflineLibraryViewModel(
            offlineRepository,
            QuestionRepository(fakeApi, tokenStorage, offlineRepository)
        ) { false }
    }

    private class FakeApiService : ApiService {
        var lastQuestionRequest: QuestionCreateRequestDto? = null

        override suspend fun login(request: LoginRequestDto): Response<LoginResponseDto> = Response.success(
            LoginResponseDto(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                tokenType = "Bearer",
                userId = "student-id",
                mfaRequired = false,
                mfaSessionToken = null,
                email = request.email,
                accountStatus = "ACTIVE",
                expiresIn = 900
            )
        )

        override suspend fun requestPasswordReset(request: PasswordRecoveryRequestDto): Response<Void> =
            Response.success(204, null as Void?)

        override suspend fun resetPassword(request: PasswordResetRequestDto): Response<Void> =
            Response.success(204, null as Void?)

        override suspend fun logout(request: LogoutRequestDto): Response<Void> =
            Response.success(204, null as Void?)

        override suspend fun verifyMfa(request: MfaVerifyRequestDto): Response<MfaVerifyResponseDto> = Response.success(
            MfaVerifyResponseDto("student-id", "student@example.test", "access-token", "refresh-token", "Bearer", 900)
        )

        override suspend fun register(request: RegisterRequestDto): Response<RegisterResponseDto> = Response.success(
            RegisterResponseDto("student-id", request.email, "ACTIVE", null, false, true, "registered")
        )

        override suspend fun getMyProfile(token: String): Response<StudentProfileDto> =
            Response.success(StudentProfileDto("profile-id", "student-id", "secondary", "VISUAL"))

        override suspend fun updateMyProfile(
            token: String,
            request: StudentProfileUpdateRequestDto
        ): Response<StudentProfileDto> = Response.success(
            StudentProfileDto("profile-id", "student-id", request.educationLevel, request.learningStyle)
        )

        override suspend fun logoutAllDevices(
            token: String,
            request: AccountReauthenticationRequestDto
        ): Response<Void> = Response.success(204, null as Void?)

        override suspend fun submitQuestion(
            token: String,
            request: QuestionCreateRequestDto
        ): Response<QuestionResponseDto> {
            lastQuestionRequest = request
            return Response.success(
                QuestionResponseDto(
                    questionId = UUID.randomUUID().toString(),
                    subject = request.subject,
                    questionText = request.questionText,
                    status = "ANSWERED",
                    isBookmarked = false,
                    imageStoragePaths = emptyList(),
                    answer = AnswerResponseDto(
                        answerId = UUID.randomUUID().toString(),
                        isAlternative = false,
                        structuredExplanation = StructuredExplanationDto(
                            summary = "Subtract two from both sides to isolate x.",
                            steps = listOf(ExplanationStepDto(1, "Isolate x", "Subtract two from each side.", "x = 5")),
                            finalAnswer = "x = 5",
                            keyTakeaways = listOf("Use inverse operations.")
                        )
                    ),
                    createdAt = "2026-01-01T00:00:00"
                )
            )
        }

        override suspend fun getQuestion(token: String, questionId: String): Response<QuestionResponseDto> =
            Response.success<QuestionResponseDto>(null)

        override suspend fun addBookmark(
            token: String,
            questionId: String
        ): Response<QuestionBookmarkResponseDto> =
            Response.success(QuestionBookmarkResponseDto(questionId, true))

        override suspend fun removeBookmark(
            token: String,
            questionId: String
        ): Response<QuestionBookmarkResponseDto> =
            Response.success(QuestionBookmarkResponseDto(questionId, false))

        override suspend fun listQuestions(token: String, page: Int, size: Int): Response<PageResponseDto<QuestionSummaryDto>> =
            Response.success(PageResponseDto(emptyList(), true, 0))

        override suspend fun listBookmarkedQuestions(token: String, page: Int, size: Int): Response<PageResponseDto<QuestionSummaryDto>> =
            Response.success(PageResponseDto(emptyList(), true, 0))

        override suspend fun listCompletedQuizAttempts(token: String): Response<List<QuizAttemptHistoryDto>> =
            Response.success(emptyList())

        override suspend fun listQuizzes(
            token: String,
            page: Int,
            size: Int
        ): Response<PageResponseDto<PracticeQuizDto>> =
            Response.success(PageResponseDto(emptyList(), true, 0))

        override suspend fun getQuiz(token: String, quizId: String): Response<PracticeQuizDto> =
            Response.success(PracticeQuizDto(quizId, "Mathematics", "GENERATED", emptyList(), null))

        override suspend fun getQuizForEdit(token: String, quizId: String): Response<QuizEditResponseDto> =
            Response.success(QuizEditResponseDto(quizId, "Mathematics", null, emptyList()))

        override suspend fun generateQuiz(
            token: String,
            request: QuizGenerateRequestDto
        ): Response<PracticeQuizDto> = Response.success(
            PracticeQuizDto("quiz-id", request.subject, "GENERATED", emptyList(), null)
        )

        override suspend fun updateQuiz(
            token: String,
            quizId: String,
            request: QuizUpdateRequestDto
        ): Response<PracticeQuizDto> =
            Response.success(PracticeQuizDto(quizId, request.subject, "GENERATED", emptyList(), null))

        override suspend fun deleteQuiz(token: String, quizId: String): Response<Void> =
            Response.success(204, null as Void?)

        override suspend fun startQuizAttempt(token: String, quizId: String): Response<QuizAttemptStartDto> =
            Response.success(QuizAttemptStartDto("attempt-id", quizId, null))

        override suspend fun submitQuizAttempt(
            token: String,
            quizId: String,
            attemptId: String,
            request: QuizSubmitRequestDto
        ): Response<QuizSubmitResponseDto> =
            Response.success(QuizSubmitResponseDto(attemptId, quizId, 0, request.responses.size, 0, emptyList(), null))

        override suspend fun getGuardianConsentStatus(token: String): Response<GuardianConsentResponseDto> =
            Response.success(GuardianConsentResponseDto("consent-id", "student-id", "APPROVED", null, null, true))

        override suspend fun resendGuardianConsent(token: String): Response<Void> = Response.success(204, null as Void?)

        override suspend fun getDashboardMetrics(token: String): Response<DashboardMetricsDto> =
            Response.success(DashboardMetricsDto(0, 0, 0.0, emptyList<SubjectBreakdownDto>()))

        override suspend fun getAiQuota(token: String): Response<AiQuotaResponseDto> =
            Response.success(AiQuotaResponseDto(99, 9000))

        override suspend fun getNotificationPreferences(token: String): Response<NotificationPreferencesDto> =
            Response.success(NotificationPreferencesDto(false, false, false))

        override suspend fun updateNotificationPreferences(
            token: String,
            request: NotificationPreferencesDto
        ): Response<NotificationPreferencesDto> = Response.success(request)

        override suspend fun deleteAccount(
            token: String,
            request: AccountReauthenticationRequestDto
        ): Response<Void> = Response.success(204, null as Void?)

    }
}
