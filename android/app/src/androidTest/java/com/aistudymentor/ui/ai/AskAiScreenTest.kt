package com.aistudymentor.ui.ai

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.remote.AnswerResponseDto
import com.aistudymentor.data.remote.AiQuotaResponseDto
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.QuestionCreateRequestDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.StructuredExplanationDto
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.ui.theme.StudyMentorTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class AskAiScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: StudyMentorDatabase
    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var request: AtomicReference<QuestionCreateRequestDto?>
    private lateinit var viewModel: AskAiViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyMentorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tokenStorage = SecureTokenStorage(context)
        tokenStorage.saveSession("ask-access", "ask-refresh", "student-id")
        request = AtomicReference()
        val api = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getAiQuota" -> Response.success(AiQuotaResponseDto(99, 9000))
                "submitQuestion" -> {
                    val question = args[1] as QuestionCreateRequestDto
                    request.set(question)
                    Response.success(
                        QuestionResponseDto(
                            questionId = "question-1",
                            subject = question.subject,
                            questionText = question.questionText,
                            status = "ANSWERED",
                            isBookmarked = false,
                            imageStoragePaths = emptyList(),
                            answer = AnswerResponseDto(
                                "answer-1",
                                false,
                                StructuredExplanationDto(
                                    "Add forty and two.",
                                    emptyList(),
                                    "42",
                                    listOf("Add the numbers.")
                                )
                            ),
                            createdAt = "2026-01-01T00:00:00"
                        )
                    )
                }
                else -> error("Unexpected API call: ${method.name}")
            }
        } as ApiService
        val offlineRepository = OfflineRepository(api, database, database.offlineDao(), tokenStorage)
        viewModel = AskAiViewModel(QuestionRepository(api, tokenStorage, offlineRepository))
    }

    @After
    fun tearDown() {
        tokenStorage.clear()
        database.close()
    }

    @Test
    fun submitRequiresQuestionText() {
        val darkTheme = mutableStateOf(false)
        composeRule.setContent {
            StudyMentorTheme(darkTheme = darkTheme.value) { AskAiScreen(viewModel, onNavigateToAnswer = { _ -> }) }
        }

        composeRule.onNodeWithText("Solve problem").assertIsNotEnabled()
        saveScreenshot("p18-ask-light.png")
        darkTheme.value = true
        composeRule.waitForIdle()
        saveScreenshot("p18-ask-dark.png")
    }

    @Test
    fun submitsQuestionAndCachesTheAnswerBeforeNavigation() {
        val navigated = AtomicReference(false)
        composeRule.setContent {
            StudyMentorTheme {
                AskAiScreen(viewModel, onNavigateToAnswer = { navigated.set(true) })
            }
        }

        composeRule.onNodeWithText("Ask a study question or paste problem text")
            .performTextInput("Solve 40 + 2")
        composeRule.onNodeWithText("Solve problem").performClick()
        composeRule.waitUntil(5_000) { navigated.get() }

        assertEquals("Mathematics", request.get()?.subject)
        assertEquals("Solve 40 + 2", request.get()?.questionText)
        runBlocking {
            assertNotNull(database.offlineDao().getQuestion("student-id", "question-1"))
        }
    }

    @Test
    fun recentQuestionClickOpensSavedQuestionById() {
        val openedQuestionId = AtomicReference<String?>()
        val recentQuestion = CachedQuestionEntity(
            accountId = "student-id",
            questionId = "saved-question-7",
            subject = "Mathematics",
            questionSnippet = "Saved question from history",
            hasImage = false,
            isBookmarked = false,
            status = "ANSWERED",
            createdAt = null
        )
        composeRule.setContent {
            StudyMentorTheme {
                AskAiScreen(
                    viewModel = viewModel,
                    onNavigateToAnswer = { _ -> },
                    onOpenQuestion = { openedQuestionId.set(it) },
                    recentQuestions = listOf(recentQuestion)
                )
            }
        }

        composeRule.onNodeWithText("Saved question from history").performClick()

        assertEquals("saved-question-7", openedQuestionId.get())
    }

    private fun saveScreenshot(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.getExternalFilesDir("P18"), "screenshots")
        check(directory.mkdirs() || directory.isDirectory)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(directory, name)).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
