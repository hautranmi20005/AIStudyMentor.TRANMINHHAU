package com.aistudymentor.ui.quizzes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.QuizEditQuestionDto
import com.aistudymentor.data.remote.QuizEditResponseDto
import com.aistudymentor.data.remote.QuizAnswerDto
import com.aistudymentor.data.remote.QuizAttemptHistoryDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizSubmitRequestDto
import com.aistudymentor.data.remote.QuizSubmitResponseDto
import com.aistudymentor.data.remote.QuizUpdateRequestDto
import com.aistudymentor.data.remote.QuestionSummaryDto
import com.aistudymentor.data.repository.ApiFailure
import com.aistudymentor.data.repository.QuizRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class QuizScreenState {
    HUB,
    EDITING,
    TAKING,
    RESULT
}

data class QuizUiState(
    val screen: QuizScreenState = QuizScreenState.HUB,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOnline: Boolean = true,
    val quizzes: List<PracticeQuizDto> = emptyList(),
    val attempts: List<QuizAttemptHistoryDto> = emptyList(),
    val recentQuestions: List<QuestionSummaryDto> = emptyList(),
    val quiz: PracticeQuizDto? = null,
    val editQuiz: QuizEditResponseDto? = null,
    val editSubject: String = "",
    val editTopic: String = "",
    val editQuestions: List<QuizEditQuestionDto> = emptyList(),
    val attemptId: String? = null,
    val questionIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val result: QuizSubmitResponseDto? = null,
    val showSubmitConfirmation: Boolean = false,
    val unansweredCount: Int = 0,
    val alreadySubmitted: Boolean = false,
    val error: String? = null,
    val lastGenerationRequest: QuizGenerateRequestDto? = null
)

class QuizViewModel(
    private val repository: QuizRepository,
    private val savedStateHandle: SavedStateHandle,
    initialOnline: Boolean = true
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        QuizUiState(
            isOnline = initialOnline,
            questionIndex = savedStateHandle[KEY_QUESTION_INDEX] ?: 0,
            answers = savedStateHandle.get<HashMap<String, String>>(KEY_ANSWERS).orEmpty()
        )
    )
    val uiState = mutableUiState.asStateFlow()

    init {
        refresh()
        restoreInProgress()
    }

    fun setOnline(isOnline: Boolean) {
        mutableUiState.update { it.copy(isOnline = isOnline) }
        if (isOnline) refresh()
    }

    fun refresh() {
        if (!mutableUiState.value.isOnline || mutableUiState.value.isLoading) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val quizzes = repository.listQuizzes()
                val attempts = repository.listCompletedAttempts()
                val recentQuestions = repository.listRecentQuestions()
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        quizzes = quizzes,
                        attempts = attempts,
                        recentQuestions = recentQuestions
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun generateQuiz(request: QuizGenerateRequestDto) {
        if (!mutableUiState.value.isOnline || mutableUiState.value.isGenerating) return
        mutableUiState.update {
            it.copy(
                isGenerating = true,
                error = null,
                lastGenerationRequest = request,
                showSubmitConfirmation = false
            )
        }
        viewModelScope.launch {
            try {
                repository.generateQuiz(request)
                mutableUiState.update {
                    it.copy(
                        screen = QuizScreenState.HUB,
                        isGenerating = false,
                        quiz = null,
                        attemptId = null,
                        questionIndex = 0,
                        answers = emptyMap(),
                        result = null,
                        error = null
                    )
                }
                refresh()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isGenerating = false, error = exception.message) }
            }
        }
    }

    fun retryGeneration() {
        mutableUiState.value.lastGenerationRequest?.let(::generateQuiz)
    }

    fun openEditor(quizId: String) {
        if (!mutableUiState.value.isOnline || mutableUiState.value.isLoading) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val editable = repository.getQuizForEdit(quizId)
                mutableUiState.update {
                    it.copy(
                        screen = QuizScreenState.EDITING,
                        isLoading = false,
                        editQuiz = editable,
                        editSubject = editable.subject.orEmpty(),
                        editTopic = editable.topic.orEmpty(),
                        editQuestions = editable.questions.orEmpty(),
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun cancelEditing() {
        mutableUiState.update {
            it.copy(
                screen = QuizScreenState.HUB,
                editQuiz = null,
                editQuestions = emptyList(),
                error = null
            )
        }
    }

    fun updateEditSubject(subject: String) {
        mutableUiState.update { it.copy(editSubject = subject) }
    }

    fun updateEditTopic(topic: String) {
        mutableUiState.update { it.copy(editTopic = topic) }
    }

    fun updateEditPrompt(questionIndex: Int, prompt: String) {
        updateEditQuestion(questionIndex) { it.copy(prompt = prompt) }
    }

    fun updateEditExplanation(questionIndex: Int, explanation: String) {
        updateEditQuestion(questionIndex) { it.copy(explanation = explanation) }
    }

    fun updateEditOption(questionIndex: Int, optionIndex: Int, option: String) {
        updateEditQuestion(questionIndex) { question ->
            val options = question.options.toMutableList()
            val oldOption = options.getOrNull(optionIndex) ?: return@updateEditQuestion question
            options[optionIndex] = option
            question.copy(
                options = options,
                correctAnswer = if (question.correctAnswer == oldOption) option else question.correctAnswer
            )
        }
    }

    fun selectEditCorrectOption(questionIndex: Int, option: String) {
        updateEditQuestion(questionIndex) { it.copy(correctAnswer = option) }
    }

    fun addEditOption(questionIndex: Int) {
        updateEditQuestion(questionIndex) { question ->
            if (question.options.size >= 5) question else question.copy(options = question.options + "")
        }
    }

    fun removeEditOption(questionIndex: Int, optionIndex: Int) {
        updateEditQuestion(questionIndex) { question ->
            if (question.options.size <= 3 || optionIndex !in question.options.indices) {
                question
            } else {
                val options = question.options.toMutableList().apply { removeAt(optionIndex) }
                question.copy(
                    options = options,
                    correctAnswer = if (question.correctAnswer in options) question.correctAnswer
                    else options.firstOrNull().orEmpty()
                )
            }
        }
    }

    fun addEditQuestion() {
        val state = mutableUiState.value
        if (state.editQuestions.size >= 10) return
        mutableUiState.update {
            it.copy(editQuestions = it.editQuestions + QuizEditQuestionDto(
                prompt = "",
                options = listOf("", "", ""),
                correctAnswer = "",
                explanation = ""
            ))
        }
    }

    fun removeEditQuestion(questionIndex: Int) {
        mutableUiState.update { state ->
            if (questionIndex !in state.editQuestions.indices) state
            else state.copy(editQuestions = state.editQuestions.filterIndexed { index, _ -> index != questionIndex })
        }
    }

    fun canSaveEdits(): Boolean {
        val state = mutableUiState.value
        return state.editSubject.isNotBlank() && state.editSubject.length <= 100
                && state.editTopic.length <= 100
                && state.editQuestions.isNotEmpty() && state.editQuestions.size <= 10
                && state.editQuestions.all { question ->
                    val normalizedOptions = question.options.map { it.trim().lowercase(Locale.ROOT) }
                    question.prompt.isNotBlank() && question.prompt.length <= 2000
                            && question.explanation.isNotBlank() && question.explanation.length <= 2000
                            && question.options.size in 3..5
                            && question.options.all { it.isNotBlank() && it.length <= 255 }
                            && normalizedOptions.distinct().size == normalizedOptions.size
                            && question.options.any { it.trim().equals(question.correctAnswer.trim(), ignoreCase = true) }
                }
    }

    fun saveEdits() {
        val state = mutableUiState.value
        val quizId = state.editQuiz?.quizId ?: return
        if (!state.isOnline || state.isLoading || !canSaveEdits()) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.updateQuiz(
                    quizId,
                    QuizUpdateRequestDto(
                        subject = state.editSubject.trim(),
                        topic = state.editTopic.trim(),
                        questions = state.editQuestions
                    )
                )
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        screen = QuizScreenState.HUB,
                        editQuiz = null,
                        editQuestions = emptyList(),
                        error = null
                    )
                }
                refresh()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    private fun updateEditQuestion(
        questionIndex: Int,
        transform: (QuizEditQuestionDto) -> QuizEditQuestionDto
    ) {
        mutableUiState.update { state ->
            if (questionIndex !in state.editQuestions.indices) state
            else state.copy(editQuestions = state.editQuestions.mapIndexed { index, question ->
                if (index == questionIndex) transform(question) else question
            })
        }
    }

    fun deleteQuiz(quizId: String) {
        if (!mutableUiState.value.isOnline || mutableUiState.value.isLoading) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteQuiz(quizId)
                mutableUiState.update { it.copy(isLoading = false) }
                refresh()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun resumeQuiz(quizId: String) {
        if (!mutableUiState.value.isOnline) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val quiz = repository.getQuiz(quizId)
                val start = repository.startAttempt(quizId)
                val attemptId = start.attemptId ?: error("The server did not return an attempt ID.")
                val existingAnswers = if (savedStateHandle.get<String>(KEY_ATTEMPT_ID) == attemptId) {
                    savedStateHandle.get<HashMap<String, String>>(KEY_ANSWERS).orEmpty()
                } else {
                    emptyMap()
                }
                val existingIndex = if (savedStateHandle.get<String>(KEY_ATTEMPT_ID) == attemptId) {
                    savedStateHandle[KEY_QUESTION_INDEX] ?: 0
                } else {
                    0
                }
                saveAttempt(quizId, attemptId, existingAnswers)
                mutableUiState.update {
                    it.copy(
                        screen = QuizScreenState.TAKING,
                        isLoading = false,
                        isGenerating = false,
                        quiz = quiz,
                        attemptId = attemptId,
                        answers = existingAnswers,
                        questionIndex = existingIndex,
                        result = null,
                        alreadySubmitted = false,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun selectOption(questionId: String, optionId: String) {
        updateAnswer(questionId, OPTION_PREFIX + optionId)
    }

    fun setWrittenAnswer(questionId: String, answer: String) {
        updateAnswer(questionId, TEXT_PREFIX + answer)
    }

    fun selectedOption(questionId: String): String? =
        mutableUiState.value.answers[questionId]
            ?.takeIf { it.startsWith(OPTION_PREFIX) }
            ?.removePrefix(OPTION_PREFIX)

    fun writtenAnswer(questionId: String): String =
        mutableUiState.value.answers[questionId]
            ?.takeIf { it.startsWith(TEXT_PREFIX) }
            ?.removePrefix(TEXT_PREFIX)
            .orEmpty()

    fun previousQuestion() {
        mutableUiState.update { state ->
            val index = (state.questionIndex - 1).coerceAtLeast(0)
            savedStateHandle[KEY_QUESTION_INDEX] = index
            state.copy(questionIndex = index)
        }
    }

    fun nextQuestion() {
        mutableUiState.update { state ->
            val lastIndex = (state.quiz?.questions?.size ?: 1) - 1
            val index = (state.questionIndex + 1).coerceAtMost(lastIndex)
            savedStateHandle[KEY_QUESTION_INDEX] = index
            state.copy(questionIndex = index)
        }
    }

    fun requestSubmit() {
        val state = mutableUiState.value
        val questions = state.quiz?.questions.orEmpty()
        val unanswered = questions.count { question ->
            val answer = state.answers[question.questionId]
            answer == null || (answer.startsWith(TEXT_PREFIX) && answer.removePrefix(TEXT_PREFIX).isBlank())
        }
        mutableUiState.update {
            it.copy(showSubmitConfirmation = true, unansweredCount = unanswered, error = null)
        }
    }

    fun dismissSubmitConfirmation() {
        mutableUiState.update { it.copy(showSubmitConfirmation = false) }
    }

    fun confirmSubmit() {
        val state = mutableUiState.value
        val quiz = state.quiz ?: return
        val quizId = quiz.quizId ?: return
        val attemptId = state.attemptId ?: return
        if (!state.isOnline || state.isSubmitting) return

        val responses = quiz.questions.orEmpty().map { question ->
            val questionId = question.questionId ?: return@map null
            val answer = state.answers[questionId]
            when {
                answer == null -> QuizAnswerDto(questionId)
                answer.startsWith(OPTION_PREFIX) ->
                    QuizAnswerDto(questionId, selectedOptionId = answer.removePrefix(OPTION_PREFIX))
                answer.startsWith(TEXT_PREFIX) ->
                    QuizAnswerDto(questionId, subjectiveText = answer.removePrefix(TEXT_PREFIX))
                else -> QuizAnswerDto(questionId)
            }
        }.filterNotNull()
        mutableUiState.update { it.copy(isSubmitting = true, showSubmitConfirmation = false, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.submitAttempt(
                    quizId,
                    attemptId,
                    QuizSubmitRequestDto(responses)
                )
                showResult(result)
            } catch (exception: ApiFailure) {
                if (exception.statusCode == 409 && exception.code == "ATTEMPT_ALREADY_SUBMITTED") {
                    loadReview(quizId, attemptId, alreadySubmitted = true)
                } else {
                    mutableUiState.update { it.copy(isSubmitting = false, error = exception.message) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isSubmitting = false, error = exception.message) }
            }
        }
    }

    fun reviewAttempt(attemptId: String) {
        if (!mutableUiState.value.isOnline) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = repository.getAttemptReview(attemptId)
                val quizId = result.quizId ?: result.attemptId?.let { id ->
                    mutableUiState.value.attempts.firstOrNull { it.attemptId == id }?.quizId
                }
                val quiz = quizId?.let { repository.getQuiz(it) }
                savedStateHandle[KEY_SUBMITTED] = true
                savedStateHandle[KEY_ATTEMPT_ID] = attemptId
                savedStateHandle[KEY_QUIZ_ID] = quizId
                mutableUiState.update {
                    it.copy(
                        screen = QuizScreenState.RESULT,
                        isLoading = false,
                        quiz = quiz,
                        attemptId = attemptId,
                        result = result,
                        alreadySubmitted = true,
                        error = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun retakeQuiz() {
        val quizId = mutableUiState.value.quiz?.quizId ?: return
        startRetake(quizId)
    }

    fun newQuiz() {
        clearSavedAttempt()
        mutableUiState.update {
            it.copy(
                screen = QuizScreenState.HUB,
                quiz = null,
                attemptId = null,
                questionIndex = 0,
                answers = emptyMap(),
                result = null,
                alreadySubmitted = false,
                isSubmitting = false,
                isGenerating = false,
                error = null
            )
        }
        refresh()
    }

    private fun startRetake(quizId: String) {
        if (!mutableUiState.value.isOnline) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val quiz = repository.getQuiz(quizId)
                val attempt = repository.startAttempt(quizId)
                val attemptId = attempt.attemptId ?: error("The server did not return an attempt ID.")
                beginAttempt(quiz, quizId, emptyMap(), attemptId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    private suspend fun beginAttempt(
        quiz: PracticeQuizDto,
        quizId: String,
        answers: Map<String, String>,
        knownAttemptId: String? = null
    ) {
        val attemptId = knownAttemptId ?: repository.startAttempt(quizId).attemptId
            ?: error("The server did not return an attempt ID.")
        saveAttempt(quizId, attemptId, answers)
        mutableUiState.update {
            it.copy(
                screen = QuizScreenState.TAKING,
                isGenerating = false,
                isLoading = false,
                isSubmitting = false,
                quiz = quiz,
                attemptId = attemptId,
                questionIndex = 0,
                answers = answers,
                result = null,
                alreadySubmitted = false,
                error = null
            )
        }
        savedStateHandle[KEY_QUESTION_INDEX] = 0
    }

    private fun restoreInProgress() {
        val quizId = savedStateHandle.get<String>(KEY_QUIZ_ID) ?: return
        val attemptId = savedStateHandle.get<String>(KEY_ATTEMPT_ID) ?: return
        viewModelScope.launch {
            try {
                if (savedStateHandle.get<Boolean>(KEY_SUBMITTED) == true) {
                    val result = repository.getAttemptReview(attemptId)
                    val quiz = repository.getQuiz(quizId)
                    mutableUiState.update {
                        it.copy(
                            screen = QuizScreenState.RESULT,
                            quiz = quiz,
                            attemptId = attemptId,
                            result = result,
                            alreadySubmitted = true,
                            error = null
                        )
                    }
                } else {
                    val quiz = repository.getQuiz(quizId)
                    val start = repository.startAttempt(quizId)
                    val restoredId = start.attemptId ?: attemptId
                    val answers = savedStateHandle.get<HashMap<String, String>>(KEY_ANSWERS).orEmpty()
                    val questionIndex = savedStateHandle[KEY_QUESTION_INDEX] ?: 0
                    saveAttempt(quizId, restoredId, answers)
                    mutableUiState.update {
                        it.copy(
                            screen = QuizScreenState.TAKING,
                            quiz = quiz,
                            attemptId = restoredId,
                            answers = answers,
                            questionIndex = questionIndex,
                            error = null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.update { it.copy(error = exception.message) }
            }
        }
    }

    private suspend fun loadReview(quizId: String, attemptId: String, alreadySubmitted: Boolean) {
        try {
            val result = repository.getAttemptReview(attemptId)
            savedStateHandle[KEY_SUBMITTED] = true
            mutableUiState.update {
                it.copy(quiz = repository.getQuiz(quizId), isSubmitting = false, alreadySubmitted = alreadySubmitted)
            }
            showResult(result, alreadySubmitted)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            mutableUiState.update { it.copy(isSubmitting = false, error = exception.message) }
        }
    }

    private fun showResult(result: QuizSubmitResponseDto, alreadySubmitted: Boolean = false) {
        savedStateHandle[KEY_SUBMITTED] = true
        mutableUiState.update {
            it.copy(
                screen = QuizScreenState.RESULT,
                isSubmitting = false,
                result = result,
                alreadySubmitted = alreadySubmitted,
                error = null
            )
        }
    }

    private fun updateAnswer(questionId: String, encoded: String) {
        mutableUiState.update { state ->
            val answers = state.answers.toMutableMap().apply { put(questionId, encoded) }
            savedStateHandle[KEY_ANSWERS] = HashMap(answers)
            state.copy(answers = answers)
        }
    }

    private fun saveAttempt(quizId: String, attemptId: String, answers: Map<String, String>) {
        savedStateHandle[KEY_QUIZ_ID] = quizId
        savedStateHandle[KEY_ATTEMPT_ID] = attemptId
        savedStateHandle[KEY_SUBMITTED] = false
        savedStateHandle[KEY_ANSWERS] = HashMap(answers)
    }

    private fun clearSavedAttempt() {
        savedStateHandle.remove<String>(KEY_QUIZ_ID)
        savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
        savedStateHandle.remove<Boolean>(KEY_SUBMITTED)
        savedStateHandle.remove<HashMap<String, String>>(KEY_ANSWERS)
        savedStateHandle.remove<Int>(KEY_QUESTION_INDEX)
    }

    companion object {
        private const val KEY_QUIZ_ID = "quiz.id"
        private const val KEY_ATTEMPT_ID = "quiz.attempt"
        private const val KEY_SUBMITTED = "quiz.submitted"
        private const val KEY_ANSWERS = "quiz.answers"
        private const val KEY_QUESTION_INDEX = "quiz.question_index"
        private const val OPTION_PREFIX = "option:"
        private const val TEXT_PREFIX = "text:"
    }
}
