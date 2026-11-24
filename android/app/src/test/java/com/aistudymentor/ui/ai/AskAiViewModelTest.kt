package com.aistudymentor.ui.ai

import com.aistudymentor.data.remote.AnswerResponseDto
import com.aistudymentor.data.remote.AiQuotaResponseDto
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.StructuredExplanationDto
import com.aistudymentor.data.repository.ApiFailure
import com.aistudymentor.data.repository.QuestionDataSource
import com.aistudymentor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.collect
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskAiViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeQuestionDataSource
    private lateinit var viewModel: AskAiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeQuestionDataSource()
        viewModel = AskAiViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submissionReturnsTheAnswerAndConsumesNavigationOnce() = runTest(dispatcher) {
        viewModel.updateSubject("Physics")
        viewModel.updatePrompt("  Explain gravity  ")
        viewModel.submitQuestion()
        advanceUntilIdle()

        assertEquals("Physics", repository.lastSubject)
        assertEquals("Explain gravity", repository.lastQuestion)
        assertNotNull(viewModel.uiState.value.response)
        assertTrue(viewModel.uiState.value.navigateToAnswer)

        viewModel.consumeAnswerNavigation()
        assertFalse(viewModel.uiState.value.navigateToAnswer)
    }

    @Test
    fun blankQuestionDoesNotCallRepository() = runTest(dispatcher) {
        viewModel.submitQuestion()
        advanceUntilIdle()

        assertEquals(0, repository.submitCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.navigateToAnswer)
    }

    @Test
    fun quotaRefreshPublishesTheServerRemainingCalls() = runTest(dispatcher) {
        repository.quota = AiQuotaResponseDto(remainingCallsToday = 42, remainingTokensToday = 9000)

        viewModel.refreshDailyQuota()
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.dailyQuota?.remainingCallsToday)
        assertFalse(viewModel.uiState.value.isQuotaLoading)
    }

    @Test
    fun quotaRefreshFailureDoesNotShowQuestionSubmissionNetworkError() = runTest(dispatcher) {
        val snackbarEvents = mutableListOf<com.aistudymentor.ui.common.SnackbarEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvents.collect(snackbarEvents::add)
        }
        repository.quotaFailure = IOException("quota endpoint unavailable")

        viewModel.refreshDailyQuota()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.quotaFailed)
        assertTrue(snackbarEvents.isEmpty())
    }

    @Test
    fun providerTimeoutShowsSpecificRetryableMessage() = runTest(dispatcher) {
        val snackbarEvents = mutableListOf<com.aistudymentor.ui.common.SnackbarEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvents.collect(snackbarEvents::add)
        }
        repository.failure = ApiFailure(
            statusCode = 504,
            code = "AI_PROVIDER_TIMEOUT",
            message = "provider timed out",
            retryable = true
        )
        viewModel.updatePrompt("Solve 1 + 1")

        viewModel.submitQuestion()
        advanceUntilIdle()

        assertEquals(R.string.error_ai_timeout, snackbarEvents.single().messageRes)
        assertEquals(R.string.retry, snackbarEvents.single().retryLabelRes)
    }

    @Test
    fun failedSubmissionDoesNotNavigateToAnAnswer() = runTest(dispatcher) {
        repository.failure = IllegalStateException("test failure")
        viewModel.updatePrompt("A question")
        viewModel.submitQuestion()
        advanceUntilIdle()

        assertEquals(1, repository.submitCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.navigateToAnswer)
        assertEquals(null, viewModel.uiState.value.response)
    }

    private class FakeQuestionDataSource : QuestionDataSource {
        var submitCount = 0
        var lastSubject: String? = null
        var lastQuestion: String? = null
        var failure: Exception? = null
        var quota: AiQuotaResponseDto? = null
        var quotaFailure: Exception? = null

        override suspend fun submitQuestion(subject: String, questionText: String): QuestionResponseDto {
            submitCount++
            lastSubject = subject
            lastQuestion = questionText
            failure?.let { throw it }
            return QuestionResponseDto(
                questionId = "question-1",
                subject = subject,
                questionText = questionText,
                status = "ANSWERED",
                isBookmarked = false,
                imageStoragePaths = emptyList(),
                answer = AnswerResponseDto(
                    "answer-1",
                    false,
                    StructuredExplanationDto("An answer", emptyList(), "42", listOf("Check the result."))
                ),
                createdAt = null
            )
        }

        override suspend fun dailyQuota(): AiQuotaResponseDto? {
            quotaFailure?.let { throw it }
            return quota
        }
    }
}
