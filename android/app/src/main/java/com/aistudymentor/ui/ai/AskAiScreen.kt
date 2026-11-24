package com.aistudymentor.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.ui.common.AppCard
import com.aistudymentor.ui.common.ObserveSnackbarEvents
import com.aistudymentor.ui.common.OfflineBanner
import com.aistudymentor.ui.common.PrimaryActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiScreen(
    viewModel: AskAiViewModel,
    onNavigateToAnswer: (String?) -> Unit,
    onOpenQuestion: (String) -> Unit = {},
    recentQuestions: List<CachedQuestionEntity> = emptyList(),
    isOnline: Boolean = true,
    onNavigateToQuizzes: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Science", "History", "Literature")
    val remainingCalls = state.dailyQuota?.remainingCallsToday
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    LaunchedEffect(state.navigateToAnswer) {
        if (state.navigateToAnswer) {
            onNavigateToAnswer(state.response?.questionId)
            viewModel.consumeAnswerNavigation()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ask_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Text(
                    text = stringResource(R.string.ask_intro),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            if (!isOnline) {
                item { OfflineBanner() }
            }
            item {
                AppCard {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.daily_quota_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    when {
                                        remainingCalls != null -> stringResource(R.string.daily_quota_value, remainingCalls)
                                        state.isQuotaLoading -> stringResource(R.string.daily_quota_loading)
                                        else -> stringResource(R.string.daily_quota_unavailable)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = viewModel::refreshDailyQuota,
                                enabled = !state.isQuotaLoading,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (state.isQuotaLoading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.dashboard_refresh)
                                    )
                                }
                            }
                        }
                        if (state.isQuotaLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.subject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subject_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    viewModel.updateSubject(subject)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::updatePrompt,
                    label = { Text(stringResource(R.string.question_hint)) },
                    enabled = !state.isLoading,
                    isError = state.hasPromptError,
                    supportingText = if (state.hasPromptError) {
                        { Text(stringResource(R.string.validation_question)) }
                    } else {
                        null
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )
            }
            item {
                PrimaryActionButton(
                    onClick = viewModel::submitQuestion,
                    enabled = state.prompt.isNotBlank() && !state.isLoading,
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ask_action))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToQuizzes),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.suggestion_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.suggestion_body),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                stringResource(R.string.suggestion_action),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.recent_questions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            if (recentQuestions.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.recent_questions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentQuestions, key = { it.accountId + it.questionId }) { question ->
                    RecentQuestionCard(question, onOpenQuestion)
                }
            }
            if (state.isLoading) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.loading))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun RecentQuestionCard(question: CachedQuestionEntity, onSelect: (String) -> Unit) {
    AppCard(
        modifier = Modifier.clickable { onSelect(question.questionId) }
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(question.subject, style = MaterialTheme.typography.titleSmall)
            Text(question.questionSnippet, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
