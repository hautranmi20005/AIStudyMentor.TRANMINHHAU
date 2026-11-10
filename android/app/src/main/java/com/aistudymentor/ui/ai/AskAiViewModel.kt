package com.aistudymentor.ui.ai

import com.aistudymentor.data.remote.QuestionResponseDto
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AskAiUiState(
    val prompt: String = "",
    val subject: String = "Mathematics",
    val isLoading: Boolean = false,
    val response: QuestionResponseDto? = null,
    val navigateToAnswer: Boolean = false
)

class AskAiViewModel(private val repository: QuestionRepository) : NetworkViewModel() {
    private val mutableUiState = MutableStateFlow(AskAiUiState())
    val uiState = mutableUiState.asStateFlow()

    fun updatePrompt(value: String) = mutableUiState.update { it.copy(prompt = value) }
    fun updateSubject(value: String) = mutableUiState.update { it.copy(subject = value) }

    fun submitQuestion() {
        val snapshot = mutableUiState.value
        if (snapshot.prompt.isBlank()) {
            emitSnackbar("Enter a question before submitting.")
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