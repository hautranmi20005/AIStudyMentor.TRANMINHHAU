package com.aistudymentor.ui.ai

import com.aistudymentor.R
import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.remote.AiQuotaResponseDto
import com.aistudymentor.data.repository.QuestionDataSource
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AskAiUiState(
    val prompt: String = "",
    val subject: String = "Mathematics",
    val hasPromptError: Boolean = false,
    val isLoading: Boolean = false,
    val isQuotaLoading: Boolean = false,
    val quotaFailed: Boolean = false,
    val dailyQuota: AiQuotaResponseDto? = null,
    val response: QuestionResponseDto? = null,
    val navigateToAnswer: Boolean = false
)

class AskAiViewModel(private val repository: QuestionDataSource) : NetworkViewModel() {
    private val mutableUiState = MutableStateFlow(AskAiUiState())
    val uiState = mutableUiState.asStateFlow()

    fun updatePrompt(value: String) = mutableUiState.update { it.copy(prompt = value, hasPromptError = false) }
    fun updateSubject(value: String) = mutableUiState.update { it.copy(subject = value) }

    fun refreshDailyQuota() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isQuotaLoading = loading) } },
            retry = { refreshDailyQuota() },
            showFailureMessage = false,
            action = {
                repository.dailyQuota()?.let { quota ->
                    mutableUiState.update { it.copy(dailyQuota = quota, quotaFailed = false) }
                }
            },
            onFailure = { mutableUiState.update { it.copy(quotaFailed = true) } }
        )
    }

    fun submitQuestion() {
        val snapshot = mutableUiState.value
        if (snapshot.prompt.isBlank()) {
            mutableUiState.update { it.copy(hasPromptError = true) }
            emitSnackbar(R.string.validation_question)
            return
        }
        requestQuestion(snapshot.subject, snapshot.prompt.trim())
    }

    private fun requestQuestion(subject: String, prompt: String) {
        launchRequest(
            setLoading = { loading ->
                mutableUiState.update { it.copy(isLoading = loading, response = if (loading) null else it.response) }
            },
            retry = { requestQuestion(subject, prompt) },
            action = {
                val response = repository.submitQuestion(subject, prompt)
                mutableUiState.update { it.copy(response = response, navigateToAnswer = true) }
                refreshDailyQuota()
            }
        )
    }

    fun consumeAnswerNavigation() {
        mutableUiState.update { it.copy(navigateToAnswer = false) }
    }

    fun clearQuestion() {
        mutableUiState.update { it.copy(prompt = "", response = null) }
    }
}