package com.aistudymentor.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: OfflineLibraryViewModel, onOpenQuestion: (String) -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(R.string.history_questions, R.string.history_bookmarks, R.string.history_results)
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    LaunchedEffect(viewModel) { viewModel.synchronize() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).height(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = viewModel::synchronize) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.history_sync))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(stringResource(title)) }
                    )
                }
            }
            when (selectedTab) {
                0 -> QuestionList(state.questions, onOpenQuestion)
                1 -> BookmarkList(state.bookmarks, onOpenQuestion)
                else -> QuizResultList(state.quizResults)
            }
        }
    }
}

@Composable
private fun QuestionList(questions: List<CachedQuestionEntity>, onOpenQuestion: (String) -> Unit) {
    if (questions.isEmpty()) {
        EmptyMessage(stringResource(R.string.history_empty_questions))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(questions, key = { it.accountId + it.questionId }) { question ->
            HistoryCard(
                title = question.subject.ifBlank { stringResource(R.string.history_question_default) },
                detail = question.questionSnippet,
                metadata = listOfNotNull(
                    question.status.takeIf(String::isNotBlank),
                    if (question.hasImage) stringResource(R.string.history_photo_question) else null,
                    question.createdAt
                ).joinToString(" · "),
                onClick = { onOpenQuestion(question.questionId) }
            )
        }
    }
}

@Composable
private fun BookmarkList(bookmarks: List<CachedBookmarkEntity>, onOpenQuestion: (String) -> Unit) {
    if (bookmarks.isEmpty()) {
        EmptyMessage(stringResource(R.string.history_empty_bookmarks))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(bookmarks, key = { it.accountId + it.questionId }) { bookmark ->
            HistoryCard(
                title = bookmark.subject.ifBlank { stringResource(R.string.history_bookmark_default) },
                detail = bookmark.questionSnippet,
                metadata = listOfNotNull(
                    bookmark.status.takeIf(String::isNotBlank),
                    if (bookmark.hasImage) stringResource(R.string.history_photo_question) else null,
                    bookmark.createdAt
                ).joinToString(" · "),
                onClick = { onOpenQuestion(bookmark.questionId) }
            )
        }
    }
}

@Composable
private fun QuizResultList(results: List<CachedQuizResultEntity>) {
    if (results.isEmpty()) {
        EmptyMessage(stringResource(R.string.history_empty_results))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(results, key = { it.accountId + it.attemptId }) { result ->
            HistoryCard(
                title = result.subject.ifBlank { stringResource(R.string.history_quiz_default) },
                detail = stringResource(
                    R.string.history_quiz_score,
                    result.correctCount,
                    result.totalQuestions,
                    result.score
                ),
                metadata = listOfNotNull(stringResource(R.string.history_completed), result.submittedAt).joinToString(" · ")
            )
        }
    }
}

@Composable
private fun HistoryCard(title: String, detail: String, metadata: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyLarge)
            if (metadata.isNotBlank()) Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.history_offline_sync_hint), style = MaterialTheme.typography.bodySmall)
    }
}
