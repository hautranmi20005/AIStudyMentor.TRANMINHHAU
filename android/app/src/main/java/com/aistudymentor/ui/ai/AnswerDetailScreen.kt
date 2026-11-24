package com.aistudymentor.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aistudymentor.R
import com.aistudymentor.data.remote.StructuredExplanationDto
import com.aistudymentor.ui.history.OfflineLibraryViewModel
import com.aistudymentor.ui.history.QuestionHistoryDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerDetailScreen(
    explanation: StructuredExplanationDto?,
    onBack: () -> Unit,
    isDemo: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.explanation_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (explanation == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.answer_unavailable), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("answer-detail-list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (isDemo) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                stringResource(R.string.ai_demo_response_label),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.problem_summary),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(explanation.summary.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                }
                items(explanation.steps.orEmpty().size) { index ->
                    val step = explanation.steps.orEmpty()[index]
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.step_title, step.stepNumber, step.title.orEmpty()),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(step.explanation.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                            step.formula?.takeIf(String::isNotBlank)?.let { formula ->
                                Text(
                                    formula,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }

                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.final_answer),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                explanation.finalAnswer.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (!explanation.keyTakeaways.isNullOrEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.key_takeaways),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    items(explanation.keyTakeaways.size) { index ->
                        Text(explanation.keyTakeaways[index], style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerDetailScreen(
    questionId: String,
    viewModel: OfflineLibraryViewModel,
    onBack: () -> Unit
) {
    QuestionHistoryDetailScreen(
        questionId = questionId,
        viewModel = viewModel,
        onBack = onBack
    )
}
