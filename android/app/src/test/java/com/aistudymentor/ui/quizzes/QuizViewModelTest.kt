package com.aistudymentor.ui.quizzes

import androidx.lifecycle.SavedStateHandle
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.QuizAttemptStartDto
import com.aistudymentor.data.remote.QuizEditQuestionDto
import com.aistudymentor.data.remote.QuizEditResponseDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizOptionDto
import com.aistudymentor.data.remote.PracticeQuizQuestionDto
import com.aistudymentor.data.remote.QuizResultItemDto
import com.aistudymentor.data.remote.QuizSubmitRequestDto
import com.aistudymentor.data.remote.QuizSubmitResponseDto
import com.aistudymentor.data.remote.QuizUpdateRequestDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.repository.QuizRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @Test
    fun savesPageAndAnswersForProcessRestoration() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeQuizRepository()
            val handle = SavedStateHandle()
            val viewModel = QuizViewModel(repository, handle)
            advanceUntilIdle()

            viewModel.generateQuiz(QuizGenerateRequestDto(subject = "Mathematics", numQuestions = 3))
            advanceUntilIdle()
            assertEquals(QuizScreenState.HUB, viewModel.uiState.value.screen)
            viewModel.resumeQuiz("quiz-1")
            advanceUntilIdle()
            val questionId = repository.quiz.questions!!.first().questionId!!
            val optionId = repository.quiz.questions!!.first().options!!.first().optionId!!
            viewModel.selectOption(questionId, optionId)
            viewModel.nextQuestion()

            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
            val restoredViewModel = QuizViewModel(repository, restoredHandle)
            advanceUntilIdle()

            assertEquals(QuizScreenState.TAKING, restoredViewModel.uiState.value.screen)
            assertEquals(1, restoredViewModel.uiState.value.questionIndex)
            assertEquals(optionId, restoredViewModel.selectedOption(questionId))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun generationIsBlockedOfflineAndUnansweredCountIsShownBeforeSubmit() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeQuizRepository()
            val viewModel = QuizViewModel(repository, SavedStateHandle())
            viewModel.setOnline(false)
            viewModel.generateQuiz(QuizGenerateRequestDto(subject = "Mathematics"))
            advanceUntilIdle()
            assertEquals(0, repository.generationCount)

            viewModel.setOnline(true)
            advanceUntilIdle()
            viewModel.generateQuiz(QuizGenerateRequestDto(subject = "Mathematics"))
            advanceUntilIdle()
            viewModel.resumeQuiz("quiz-1")
            advanceUntilIdle()
            viewModel.requestSubmit()
            assertEquals(2, viewModel.uiState.value.unansweredCount)
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun editorSupportsQuestionChangesAndSavesTheCompleteDraft() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeQuizRepository()
            val viewModel = QuizViewModel(repository, SavedStateHandle())
            advanceUntilIdle()

            viewModel.openEditor("quiz-1")
            advanceUntilIdle()
            assertEquals(QuizScreenState.EDITING, viewModel.uiState.value.screen)
            viewModel.updateEditPrompt(0, "Updated question")
            viewModel.updateEditExplanation(0, "Updated explanation")
            viewModel.addEditQuestion()
            assertEquals(2, viewModel.uiState.value.editQuestions.size)
            viewModel.removeEditQuestion(1)
            assertEquals(1, viewModel.uiState.value.editQuestions.size)
            assertEquals(true, viewModel.canSaveEdits())
            viewModel.saveEdits()
            advanceUntilIdle()

            assertEquals(QuizScreenState.HUB, viewModel.uiState.value.screen)
            assertEquals("Updated question", repository.updatedRequest?.questions?.single()?.prompt)
            assertEquals("Updated explanation", repository.updatedRequest?.questions?.single()?.explanation)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeQuizRepository : QuizRepository {
        var generationCount = 0
        var submittedRequest: QuizSubmitRequestDto? = null
        var updatedRequest: QuizUpdateRequestDto? = null
        val quiz = PracticeQuizDto(
            quizId = "quiz-1",
            subject = "Mathematics",
            status = "GENERATED",
            questions = listOf(
                PracticeQuizQuestionDto("question-1", "MCQ", "What is 2 + 2?",
                    listOf(QuizOptionDto("option-1", "4", 1), QuizOptionDto("option-2", "5", 2),
                        QuizOptionDto("option-3", "6", 3))),
                PracticeQuizQuestionDto("question-2", "FILL_IN_THE_BLANK", "What is 3 + 3?", emptyList())
            ),
            createdAt = null
        )

        override suspend fun listQuizzes() = emptyList<PracticeQuizDto>()
        override suspend fun listCompletedAttempts() = emptyList<QuizAttemptHistoryDto>()
        override suspend fun listRecentQuestions() = emptyList<QuestionSummaryDto>()
        override suspend fun getQuiz(quizId: String) = quiz
        override suspend fun getQuizForEdit(quizId: String) = QuizEditResponseDto(
            quizId = quizId,
            subject = quiz.subject,
            topic = "Algebra",
            questions = listOf(QuizEditQuestionDto(
                prompt = "What is 2 + 2?",
                options = listOf("4", "5", "6"),
                correctAnswer = "4",
                explanation = "Add two and two."
            ))
        )
        override suspend fun generateQuiz(request: QuizGenerateRequestDto): PracticeQuizDto {
            generationCount++
            return quiz
        }
        override suspend fun updateQuiz(quizId: String, request: QuizUpdateRequestDto): PracticeQuizDto {
            updatedRequest = request
            return quiz.copy(subject = request.subject, topic = request.topic)
        }
        override suspend fun deleteQuiz(quizId: String) = Unit
        override suspend fun startAttempt(quizId: String) =
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
                score = 50,
                totalQuestions = 2,
                correctCount = 1,
                results = listOf(QuizResultItemDto("question-1", false, "4", "Explain"),
                    QuizResultItemDto("question-2", true, "6", "Explain")),
                submittedAt = null
            )
        }
        override suspend fun getAttemptReview(attemptId: String) =
            QuizSubmitResponseDto(attemptId, score = 50, totalQuestions = 2, correctCount = 1,
                results = emptyList(), submittedAt = null)
    }
}
