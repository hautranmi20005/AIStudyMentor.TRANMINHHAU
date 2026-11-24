package com.aistudymentor.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aistudymentor.R
import com.aistudymentor.data.remote.AnswerResponseDto
import com.aistudymentor.ui.common.ObserveSnackbarEvents
import com.google.gson.Gson
import com.google.gson.JsonParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionHistoryDetailScreen(
    questionId: String,
    viewModel: OfflineLibraryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbarHostState)
    LaunchedEffect(questionId) { viewModel.openQuestion(questionId) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        enabled = !state.isBookmarkSaving,
                        onClick = {
                            state.detail?.let { viewModel.setBookmark(questionId, !it.isBookmarked) }
                        }
                    ) {
                        Icon(
                            if (state.detail?.isBookmarked == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(
                                if (state.detail?.isBookmarked == true) R.string.remove_bookmark else R.string.add_bookmark
                            )
                        )
                    }
                }
            )
        }
    ) { padding ->
        val question = state.detail
        if (question == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                if (state.isDetailLoading) CircularProgressIndicator(Modifier.padding(24.dp))
                else Text(stringResource(R.string.question_not_available_offline), Modifier.padding(24.dp))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (state.isOnline) stringResource(R.string.online_saved_question)
                    else stringResource(R.string.offline_saved_question),
                    color = if (state.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.pendingBookmarkCount > 0) {
                    Text(
                        stringResource(R.string.bookmark_pending_sync),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(question.subject, style = MaterialTheme.typography.headlineSmall)
                if (question.questionText.isNotBlank()) {
                    Text(question.questionText, style = MaterialTheme.typography.bodyLarge)
                }
                question.imageUrl?.let { image ->
                    AsyncImage(
                        model = image,
                        contentDescription = stringResource(R.string.question_image),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
                val answerResponse = remember(question.answerJson) {
                    question.answerJson?.let {
                        try {
                            Gson().fromJson<AnswerResponseDto>(it, AnswerResponseDto::class.java)
                        } catch (_: JsonParseException) {
                            null
                        }
                    }
                }
                val answer = answerResponse?.structuredExplanation
                when {
                    answer != null -> {
                        Text(stringResource(R.string.answer_title), style = MaterialTheme.typography.titleLarge)
                        if (answerResponse.isDemo) {
                            Text(
                                stringResource(R.string.ai_demo_response_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        answer.summary?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        answer.steps.orEmpty().forEachIndexed { index, step ->
                            Text(
                                "${index + 1}. ${step.title.orEmpty()}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            step.explanation?.let { Text(it) }
                            step.formula?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        answer.finalAnswer?.let { finalAnswer ->
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.final_answer), style = MaterialTheme.typography.titleMedium)
                            Text(finalAnswer, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    question.answerJson != null -> Text(
                        stringResource(R.string.answer_unreadable),
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> Text(stringResource(R.string.answer_not_synced))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isDetailLoading) CircularProgressIndicator()
                    if (state.isBookmarkSaving) Text(stringResource(R.string.bookmark_saving))
                }
            }
        }
    }
}
