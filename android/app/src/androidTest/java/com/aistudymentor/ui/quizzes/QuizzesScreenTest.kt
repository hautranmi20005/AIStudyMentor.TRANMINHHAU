package com.aistudymentor.ui.quizzes

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.PracticeQuizQuestionDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.QuizAttemptStartDto
import com.aistudymentor.data.remote.QuizEditQuestionDto
import com.aistudymentor.data.remote.QuizEditResponseDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizOptionDto
import com.aistudymentor.data.remote.QuizResultItemDto
import com.aistudymentor.data.remote.QuizSubmitRequestDto
import com.aistudymentor.data.remote.QuizSubmitResponseDto
import com.aistudymentor.data.remote.QuizUpdateRequestDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.repository.QuizRepository
import com.aistudymentor.ui.theme.StudyMentorTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuizzesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeQuizRepository()
    private val viewModel = QuizViewModel(repository, SavedStateHandle())

    @Test
    fun rendersPracticeQuizEntryPoint() {
        val darkTheme = mutableStateOf(false)
        composeRule.setContent {
            StudyMentorTheme(darkTheme = darkTheme.value) {
                QuizzesScreen(viewModel)
            }
        }

        composeRule.onNodeWithText("Practice quizzes").assertIsDisplayed()
        composeRule.onNodeWithText("Ready to test your knowledge?").assertIsDisplayed()
        composeRule.onNodeWithText("Generate a practice quiz").assertIsDisplayed()
        saveScreenshot("p18-quiz-light.png")
        darkTheme.value = true
        composeRule.waitForIdle()
        saveScreenshot("p18-quiz-dark.png")
    }

    @Test
    fun generatesAnswersAndSubmitsPracticeQuiz() {
        composeRule.setContent {
            StudyMentorTheme { QuizzesScreen(viewModel) }
        }

        composeRule.onNodeWithText("Generate a practice quiz").performClick()
        composeRule.waitUntil(5_000) {
            viewModel.uiState.value.screen == QuizScreenState.HUB && viewModel.uiState.value.quizzes.isNotEmpty()
        }
        composeRule.onNodeWithText("Start").performClick()
        composeRule.waitUntil(5_000) { viewModel.uiState.value.screen == QuizScreenState.TAKING }
        composeRule.onNodeWithText("13").performClick()
        composeRule.onNodeWithText("Submit quiz").performClick()
        composeRule.onNodeWithText("Submit answers").performClick()
        composeRule.waitUntil(5_000) { viewModel.uiState.value.screen == QuizScreenState.RESULT }

        composeRule.onNodeWithText("100% correct").assertIsDisplayed()
        assertEquals("option-13", repository.submittedRequest?.responses?.single()?.selectedOptionId)
    }

    @Test
    fun generatedQuizQuestionsCanBeEditedBeforeStarting() {
        composeRule.setContent {
            StudyMentorTheme { QuizzesScreen(viewModel) }
        }

        composeRule.onNodeWithText("Generate a practice quiz").performClick()
        composeRule.waitUntil(5_000) {
            viewModel.uiState.value.quizzes.isNotEmpty() && !viewModel.uiState.value.isLoading
        }
        composeRule.onNodeWithText("Edit").performClick()
        composeRule.waitUntil(5_000) { viewModel.uiState.value.screen == QuizScreenState.EDITING }
        composeRule.onNodeWithText("Question").performTextReplacement("What is 6 plus 7?")
        composeRule.onNodeWithText("Explanation").performTextReplacement("Add six and seven.")
        composeRule.onNodeWithText("Add question").performClick()
        composeRule.onAllNodesWithText("Remove question").get(1).performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(5_000) {
            repository.updatedRequest != null && viewModel.uiState.value.screen == QuizScreenState.HUB
        }
        assertEquals("Ancient Rome", repository.updatedRequest?.topic)
        assertEquals("What is 6 plus 7?", repository.updatedRequest?.questions?.single()?.prompt)
        assertEquals("Add six and seven.", repository.updatedRequest?.questions?.single()?.explanation)

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onAllNodesWithText("Delete").get(1).performClick()
        composeRule.waitUntil(5_000) {
            viewModel.uiState.value.quizzes.isEmpty() && !viewModel.uiState.value.isLoading
        }
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

    private class FakeQuizRepository : QuizRepository {
        var submittedRequest: QuizSubmitRequestDto? = null
        var updatedRequest: QuizUpdateRequestDto? = null
        var quizCreated = false
        var quiz = PracticeQuizDto(
            quizId = "quiz-1",
            subject = "Mathematics",
            status = "GENERATED",
            questions = listOf(
                PracticeQuizQuestionDto(
                    questionId = "question-1",
                    type = "MCQ",
                    prompt = "What is 6 plus 7?",
                    options = listOf(
                        QuizOptionDto("option-13", "13", 1),
                        QuizOptionDto("option-42", "42", 2)
                    )
                )
            ),
            createdAt = null
        )

        override suspend fun listQuizzes(): List<PracticeQuizDto> = if (quizCreated) listOf(quiz) else emptyList()
        override suspend fun listCompletedAttempts(): List<QuizAttemptHistoryDto> = emptyList()
        override suspend fun listRecentQuestions(): List<QuestionSummaryDto> = emptyList()
        override suspend fun getQuiz(quizId: String): PracticeQuizDto = quiz
        override suspend fun getQuizForEdit(quizId: String): QuizEditResponseDto =
            QuizEditResponseDto(
                quizId = quizId,
                subject = quiz.subject,
                topic = "Addition",
                questions = listOf(QuizEditQuestionDto(
                    prompt = "What is 6 plus 7?",
                    options = listOf("13", "42", "12"),
                    correctAnswer = "13",
                    explanation = "Combine the numbers."
                ))
            )
        override suspend fun generateQuiz(request: QuizGenerateRequestDto): PracticeQuizDto =
            quiz.also { quizCreated = true }
        override suspend fun updateQuiz(quizId: String, request: QuizUpdateRequestDto) =
            quiz.copy(subject = request.subject, topic = request.topic).also {
                updatedRequest = request
                quiz = it
            }
        override suspend fun deleteQuiz(quizId: String) {
            quizCreated = false
        }
        override suspend fun startAttempt(quizId: String): QuizAttemptStartDto =
            QuizAttemptStartDto("attempt-1", quizId, null)
        override suspend fun submitAttempt(
            quizId: String,
            attemptId: String,
            request: QuizSubmitRequestDto
        ): QuizSubmitResponseDto {
            submittedRequest = request
            return QuizSubmitResponseDto(
                attemptId = attemptId,
                quizId = quizId,
                score = 100,
                totalQuestions = 1,
                correctCount = 1,
                results = listOf(QuizResultItemDto("question-1", true, "13", "Add the numbers.")),
                submittedAt = null
            )
        }
        override suspend fun getAttemptReview(attemptId: String): QuizSubmitResponseDto =
            error("Unexpected attempt review")
    }
}
