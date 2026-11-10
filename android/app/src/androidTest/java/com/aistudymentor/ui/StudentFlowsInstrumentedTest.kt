package com.aistudymentor.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import androidx.room.Room
import androidx.test.espresso.Espresso
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.AnswerResponseDto
import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.remote.ExplanationStepDto
import com.aistudymentor.data.remote.GuardianConsentResponseDto
import com.aistudymentor.data.remote.LoginRequestDto
import com.aistudymentor.data.remote.LoginResponseDto
import com.aistudymentor.data.remote.MfaVerifyRequestDto
import com.aistudymentor.data.remote.MfaVerifyResponseDto
import com.aistudymentor.data.remote.PageResponseDto
import com.aistudymentor.data.remote.QuestionCreateRequestDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.RegisterRequestDto
import com.aistudymentor.data.remote.RegisterResponseDto
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
        composeRule.onNodeWithText("Sign in").performClick()
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

        composeRule.onNodeWithText("Ask your study question or paste problem text...")
            .performTextInput("Solve x + 2 = 7")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Solve Problem").performClick()
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
        composeRule.onNodeWithText("Step 1: Isolate x").assertIsDisplayed()
        composeRule.onNodeWithText("Subtract two from each side.").assertIsDisplayed()
        composeRule.onAllNodesWithText("x = 5").assertCountEquals(2)
        composeRule.onNodeWithText("• Use inverse operations.").assertIsDisplayed()
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

        override suspend fun verifyMfa(request: MfaVerifyRequestDto): Response<MfaVerifyResponseDto> = Response.success(
            MfaVerifyResponseDto("student-id", "student@example.test", "access-token", "refresh-token", "Bearer", 900)
        )

        override suspend fun register(request: RegisterRequestDto): Response<RegisterResponseDto> = Response.success(
            RegisterResponseDto("student-id", request.email, "ACTIVE", null, false, true, "registered")
        )

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
            Response.success(null)

        override suspend fun listQuestions(token: String, page: Int, size: Int): Response<PageResponseDto<QuestionSummaryDto>> =
            Response.success(PageResponseDto(emptyList(), true, 0))

        override suspend fun listBookmarkedQuestions(token: String, page: Int, size: Int): Response<PageResponseDto<QuestionSummaryDto>> =
            Response.success(PageResponseDto(emptyList(), true, 0))

        override suspend fun listCompletedQuizAttempts(token: String): Response<List<QuizAttemptHistoryDto>> =
            Response.success(emptyList())

        override suspend fun getGuardianConsentStatus(token: String): Response<GuardianConsentResponseDto> =
            Response.success(GuardianConsentResponseDto("consent-id", "student-id", "APPROVED", null, null, true))

        override suspend fun resendGuardianConsent(token: String): Response<Void> = Response.success(204, null as Void?)

        override suspend fun getDashboardMetrics(token: String): Response<DashboardMetricsDto> =
            Response.success(DashboardMetricsDto(0, 0, 0.0, emptyList<SubjectBreakdownDto>()))
    }
}
