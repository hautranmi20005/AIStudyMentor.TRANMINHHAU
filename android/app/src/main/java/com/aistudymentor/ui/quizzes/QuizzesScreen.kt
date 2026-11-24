package com.aistudymentor.ui.quizzes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.data.remote.PracticeQuizDto
import com.aistudymentor.data.remote.PracticeQuizQuestionDto
import com.aistudymentor.data.remote.QuizEditQuestionDto
import com.aistudymentor.data.remote.QuizGenerateRequestDto
import com.aistudymentor.data.remote.QuizResultItemDto
import com.aistudymentor.ui.common.EmptyState
import com.aistudymentor.ui.common.ErrorState
import com.aistudymentor.ui.common.LoadingState
import com.aistudymentor.ui.common.OfflineBanner
import com.aistudymentor.ui.common.PrimaryActionButton
import com.aistudymentor.ui.common.SecondaryActionButton

private data class QuizSubject(val value: String, val labelRes: Int)

private val quizSubjects = listOf(
    QuizSubject("Mathematics", R.string.subject_mathematics),
    QuizSubject("Physics", R.string.subject_physics),
    QuizSubject("Chemistry", R.string.subject_chemistry),
    QuizSubject("Biology", R.string.subject_biology),
    QuizSubject("History", R.string.subject_history)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen(viewModel: QuizViewModel, isOnline: Boolean = true) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var subject by rememberSaveable { mutableStateOf("Mathematics") }
    var topic by rememberSaveable { mutableStateOf("") }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var deletingQuiz by remember { mutableStateOf<PracticeQuizDto?>(null) }
    LaunchedEffect(isOnline) { viewModel.setOnline(isOnline) }

    if (state.showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmitConfirmation,
            title = { Text(stringResource(R.string.quiz_confirm_title)) },
            text = {
                Text(
                    if (state.unansweredCount > 0) {
                        stringResource(R.string.quiz_confirm_unanswered, state.unansweredCount)
                    } else {
                        stringResource(R.string.quiz_confirm_complete)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSubmit) {
                    Text(stringResource(R.string.quiz_confirm_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSubmitConfirmation) {
                    Text(stringResource(R.string.quiz_cancel))
                }
            }
        )
    }
    deletingQuiz?.let { quiz ->
        AlertDialog(
            onDismissRequest = { deletingQuiz = null },
            title = { Text(stringResource(R.string.quiz_delete_title)) },
            text = { Text(stringResource(R.string.quiz_delete_message)) },
            confirmButton = {
                TextButton(
                    enabled = !state.isLoading,
                    onClick = {
                        quiz.quizId?.let(viewModel::deleteQuiz)
                        deletingQuiz = null
                    }
                ) { Text(stringResource(R.string.quiz_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingQuiz = null }) {
                    Text(stringResource(R.string.quiz_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.screen == QuizScreenState.RESULT) R.string.quiz_result_title
                            else R.string.quiz_title
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            if (!state.isOnline) OfflineBanner()
            state.error?.let {
                ErrorState(
                    messageRes = R.string.quiz_error,
                    onRetry = {
                        when {
                            state.screen == QuizScreenState.EDITING -> viewModel.saveEdits()
                            state.lastGenerationRequest != null -> viewModel.retryGeneration()
                            else -> viewModel.refresh()
                        }
                    }
                )
            }
            when (state.screen) {
                QuizScreenState.HUB -> QuizHub(
                    subject = subject,
                    topic = topic,
                    expanded = subjectMenuExpanded,
                    onExpandedChange = { subjectMenuExpanded = it },
                    onSubjectSelected = {
                        subject = it
                        subjectMenuExpanded = false
                    },
                    onTopicChange = { topic = it },
                    isOnline = state.isOnline,
                    isLoading = state.isLoading,
                    isGenerating = state.isGenerating,
                    quizzes = state.quizzes,
                    onGenerate = {
                        viewModel.generateQuiz(QuizGenerateRequestDto(subject, numQuestions = 5, topic = topic))
                    },
                    onStartQuiz = viewModel::resumeQuiz,
                    onEditQuiz = viewModel::openEditor,
                    onDeleteQuiz = { deletingQuiz = it }
                )
                QuizScreenState.EDITING -> QuizEditor(
                    subject = state.editSubject,
                    topic = state.editTopic,
                    questions = state.editQuestions,
                    isLoading = state.isLoading,
                    canSave = viewModel.canSaveEdits(),
                    onSubjectChange = viewModel::updateEditSubject,
                    onTopicChange = viewModel::updateEditTopic,
                    onPromptChange = viewModel::updateEditPrompt,
                    onExplanationChange = viewModel::updateEditExplanation,
                    onOptionChange = viewModel::updateEditOption,
                    onCorrectOption = viewModel::selectEditCorrectOption,
                    onAddOption = viewModel::addEditOption,
                    onRemoveOption = viewModel::removeEditOption,
                    onAddQuestion = viewModel::addEditQuestion,
                    onRemoveQuestion = viewModel::removeEditQuestion,
                    onSave = viewModel::saveEdits,
                    onCancel = viewModel::cancelEditing
                )
                QuizScreenState.TAKING -> QuizTaking(
                    question = state.quiz?.questions?.getOrNull(state.questionIndex),
                    questionIndex = state.questionIndex,
                    questionCount = state.quiz?.questions.orEmpty().size,
                    selectedOption = state.quiz?.questions?.getOrNull(state.questionIndex)
                        ?.questionId?.let(viewModel::selectedOption),
                    writtenAnswer = state.quiz?.questions?.getOrNull(state.questionIndex)
                        ?.questionId?.let(viewModel::writtenAnswer).orEmpty(),
                    isSubmitting = state.isSubmitting,
                    onSelectOption = viewModel::selectOption,
                    onWrittenAnswer = viewModel::setWrittenAnswer,
                    onPrevious = viewModel::previousQuestion,
                    onNext = viewModel::nextQuestion,
                    onSubmit = viewModel::requestSubmit
                )
                QuizScreenState.RESULT -> QuizResult(
                    score = state.result?.score ?: 0,
                    correct = state.result?.correctCount ?: 0,
                    total = state.result?.totalQuestions ?: 0,
                    questions = state.quiz?.questions.orEmpty(),
                    results = state.result?.results.orEmpty(),
                    onNewQuiz = viewModel::newQuiz,
                    onRetake = viewModel::retakeQuiz
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizHub(
    subject: String,
    topic: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSubjectSelected: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    isOnline: Boolean,
    isLoading: Boolean,
    isGenerating: Boolean,
    quizzes: List<PracticeQuizDto>,
    onGenerate: () -> Unit,
    onStartQuiz: (String) -> Unit,
    onEditQuiz: (String) -> Unit,
    onDeleteQuiz: (PracticeQuizDto) -> Unit
) {
    Text(
        stringResource(R.string.quiz_intro),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() }
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = stringResource(quizSubjects.first { it.value == subject }.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.subject_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            quizSubjects.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = { onSubjectSelected(option.value) }
                )
            }
        }
    }
    OutlinedTextField(
        value = topic,
        onValueChange = onTopicChange,
        label = { Text(stringResource(R.string.quiz_topic_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    PrimaryActionButton(
        onClick = onGenerate,
        enabled = isOnline && !isLoading && !isGenerating,
        loading = isGenerating,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.quiz_generate))
    }
    Text(
        stringResource(R.string.quiz_your_quizzes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
    )
    if (quizzes.isEmpty() && !isLoading) {
        Text(stringResource(R.string.quiz_no_saved_quizzes), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    quizzes.forEach { quiz ->
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(quiz.subject.orEmpty(), style = MaterialTheme.typography.titleMedium)
                quiz.topic?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.quiz_status, quiz.status.orEmpty()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val quizId = quiz.quizId
                val canManage = quiz.status.equals("GENERATED", ignoreCase = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { quizId?.let(onStartQuiz) },
                        enabled = isOnline && quizId != null
                    ) { Text(stringResource(R.string.quiz_start)) }
                    TextButton(
                        onClick = { quizId?.let(onEditQuiz) },
                        enabled = isOnline && !isLoading && canManage && quizId != null
                    ) { Text(stringResource(R.string.quiz_edit)) }
                    TextButton(
                        onClick = { onDeleteQuiz(quiz) },
                        enabled = isOnline && !isLoading && canManage && quizId != null
                    ) { Text(stringResource(R.string.quiz_delete)) }
                }
                if (!canManage) {
                    Text(
                        stringResource(R.string.quiz_manage_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (!isOnline) {
        Text(stringResource(R.string.quiz_offline), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (isLoading) LoadingState(R.string.quiz_loading)
}

@Composable
private fun QuizEditor(
    subject: String,
    topic: String,
    questions: List<QuizEditQuestionDto>,
    isLoading: Boolean,
    canSave: Boolean,
    onSubjectChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onPromptChange: (Int, String) -> Unit,
    onExplanationChange: (Int, String) -> Unit,
    onOptionChange: (Int, Int, String) -> Unit,
    onCorrectOption: (Int, String) -> Unit,
    onAddOption: (Int) -> Unit,
    onRemoveOption: (Int, Int) -> Unit,
    onAddQuestion: () -> Unit,
    onRemoveQuestion: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        stringResource(R.string.quiz_edit_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() }
    )
    OutlinedTextField(
        value = subject,
        onValueChange = onSubjectChange,
        label = { Text(stringResource(R.string.quiz_subject_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = topic,
        onValueChange = onTopicChange,
        label = { Text(stringResource(R.string.quiz_topic_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    questions.forEachIndexed { questionIndex, question ->
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.quiz_edit_question_number, questionIndex + 1),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onRemoveQuestion(questionIndex) }) {
                        Text(stringResource(R.string.quiz_remove_question))
                    }
                }
                OutlinedTextField(
                    value = question.prompt,
                    onValueChange = { onPromptChange(questionIndex, it) },
                    label = { Text(stringResource(R.string.quiz_question_prompt)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.quiz_correct_option_hint))
                question.options.forEachIndexed { optionIndex, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = question.correctAnswer == option,
                            onClick = { onCorrectOption(questionIndex, option) }
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { onOptionChange(questionIndex, optionIndex, it) },
                            label = {
                                Text(stringResource(R.string.quiz_option_number, optionIndex + 1))
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (question.options.size > 3) {
                            TextButton(onClick = { onRemoveOption(questionIndex, optionIndex) }) {
                                Text(stringResource(R.string.quiz_remove_option))
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { onAddOption(questionIndex) },
                    enabled = question.options.size < 5
                ) { Text(stringResource(R.string.quiz_add_option)) }
                OutlinedTextField(
                    value = question.explanation,
                    onValueChange = { onExplanationChange(questionIndex, it) },
                    label = { Text(stringResource(R.string.quiz_explanation_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    TextButton(onClick = onAddQuestion, enabled = questions.size < 10) {
        Text(stringResource(R.string.quiz_add_question))
    }
    PrimaryActionButton(
        onClick = onSave,
        enabled = canSave && !isLoading,
        loading = isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.quiz_save))
    }
    SecondaryActionButton(onClick = onCancel, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.quiz_cancel))
    }
}

@Composable
private fun QuizTaking(
    question: PracticeQuizQuestionDto?,
    questionIndex: Int,
    questionCount: Int,
    selectedOption: String?,
    writtenAnswer: String,
    isSubmitting: Boolean,
    onSelectOption: (String, String) -> Unit,
    onWrittenAnswer: (String, String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    if (question == null || questionCount == 0) {
        EmptyState(R.string.quiz_no_questions)
        return
    }
    Text(
        stringResource(R.string.quiz_question_count, questionIndex + 1, questionCount),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question.prompt.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            if (!question.options.isNullOrEmpty()) {
                question.options.forEach { option ->
                    val questionId = question.questionId ?: return@forEach
                    val optionId = option.optionId ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelectOption(questionId, optionId)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == optionId,
                            onClick = { onSelectOption(questionId, optionId) }
                        )
                        Text(option.optionText.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                val questionId = question.questionId.orEmpty()
                OutlinedTextField(
                    value = writtenAnswer,
                    onValueChange = { onWrittenAnswer(questionId, it) },
                    label = { Text(stringResource(R.string.new_answer_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SecondaryActionButton(
            onClick = onPrevious,
            enabled = questionIndex > 0,
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.quiz_previous)) }
        if (questionIndex < questionCount - 1) {
            PrimaryActionButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.quiz_next))
            }
        } else {
            PrimaryActionButton(
                onClick = onSubmit,
                enabled = !isSubmitting,
                loading = isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.quiz_submit))
            }
        }
    }
}

@Composable
private fun QuizResult(
    score: Int,
    correct: Int,
    total: Int,
    questions: List<PracticeQuizQuestionDto>,
    results: List<QuizResultItemDto>,
    onNewQuiz: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.quiz_result_score, score),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.quiz_result_count, correct, total))
        results.forEach { result ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    questions.firstOrNull { it.questionId == result.questionId }?.prompt?.let {
                        Text(it, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        stringResource(
                            if (result.isCorrect == true) R.string.quiz_result_correct
                            else R.string.quiz_result_incorrect
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.quiz_result_answer, result.correctAnswer.orEmpty())
                    )
                    result.explanation?.takeIf(String::isNotBlank)?.let {
                        Text(stringResource(R.string.quiz_result_explanation, it))
                    }
                }
            }
        }
        PrimaryActionButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_retake))
        }
        SecondaryActionButton(onClick = onNewQuiz, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_new))
        }
    }
}
