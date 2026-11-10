package com.aistudymentor.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudymentor.data.remote.StructuredExplanationDto
import com.aistudymentor.data.remote.ExplanationStepDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerDetailScreen(explanation: StructuredExplanationDto?, onBack: () -> Unit) {
    val displayExplanation = explanation ?: StructuredExplanationDto(
        summary = "Derivative of f(x) = x^3 * sin(x) using Product Rule",
        steps = listOf(
            ExplanationStepDto(
                stepNumber = 1,
                title = "Identify the Product Rule",
                explanation = "The product rule states that (u * v)' = u' * v + u * v'. Let u = x^3 and v = sin(x).",
                formula = "d/dx [u * v] = u'v + uv'"
            ),
            ExplanationStepDto(
                stepNumber = 2,
                title = "Compute Derivatives of Components",
                explanation = "Differentiate u with respect to x: u' = 3x^2. Differentiate v with respect to x: v' = cos(x).",
                formula = "d/dx [x^3] = 3x^2, d/dx [sin(x)] = cos(x)"
            ),
            ExplanationStepDto(
                stepNumber = 3,
                title = "Substitute and Simplify",
                explanation = "Substitute u, u', v, and v' back into the product rule formula.",
                formula = "f'(x) = (3x^2)(sin(x)) + (x^3)(cos(x))"
            )
        ),
        finalAnswer = "f'(x) = 3x^2 sin(x) + x^3 cos(x)",
        keyTakeaways = listOf(
            "Always identify product components u and v clearly.",
            "Apply standard calculus derivative formulas for trigonometric and polynomial functions."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step-by-Step Explanation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("answer-detail-list"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Problem Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(displayExplanation.summary.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            items(displayExplanation.steps.orEmpty().size) { index ->
                val step = displayExplanation.steps.orEmpty()[index]
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Step ${step.stepNumber}: ${step.title.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                        Text(step.explanation.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        step.formula?.takeIf(String::isNotBlank)?.let { formula ->
                            Text(formula, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Final Answer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(displayExplanation.finalAnswer.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            if (!displayExplanation.keyTakeaways.isNullOrEmpty()) {
                item { Text("Key takeaways", style = MaterialTheme.typography.titleLarge) }
                items(displayExplanation.keyTakeaways.size) { index ->
                    Text("• ${displayExplanation.keyTakeaways[index]}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
