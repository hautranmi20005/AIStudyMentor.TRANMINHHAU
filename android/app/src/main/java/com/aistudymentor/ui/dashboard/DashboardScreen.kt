package com.aistudymentor.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.ui.common.DashboardShimmer
import com.aistudymentor.ui.common.ObserveSnackbarEvents
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbarHostState)
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.dashboard_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        when {
            state.metrics == null && state.isLoading -> DashboardShimmer(
                Modifier.fillMaxSize().padding(padding).padding(16.dp)
            )
            state.metrics == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.dashboard_unavailable), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.dashboard_retry_message))
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::refresh) { Text(stringResource(R.string.retry)) }
            }
            else -> {
                val metrics = state.metrics!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MetricCard(
                            title = stringResource(R.string.questions_asked),
                            value = metrics.totalQuestionsAsked?.toString() ?: "0",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        MetricCard(
                            title = stringResource(R.string.quizzes_completed),
                            value = metrics.totalQuizzesCompleted?.toString() ?: "0",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    item {
                        MetricCard(
                            title = stringResource(R.string.overall_accuracy),
                            value = String.format(Locale.getDefault(), "%.1f%%", metrics.overallAccuracyRate ?: 0.0),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (!metrics.subjectBreakdown.isNullOrEmpty()) {
                        item { Text(stringResource(R.string.by_subject), style = MaterialTheme.typography.titleLarge) }
                        items(metrics.subjectBreakdown) { subject ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(subject.subject ?: stringResource(R.string.subject_unknown), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(
                                            R.string.subject_activity,
                                            subject.questionsCount ?: 0,
                                            subject.quizCount ?: 0
                                        )
                                    )
                                    Text(
                                        stringResource(
                                            R.string.accuracy_value,
                                            subject.accuracyRate ?: 0.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, color = color)
        }
    }
}
