package com.aistudymentor.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.ui.common.ObserveSnackbarEvents
import com.aistudymentor.ui.common.ShimmerBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiScreen(viewModel: AskAiViewModel, onNavigateToAnswer: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "History")
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    LaunchedEffect(state.navigateToAnswer) {
        if (state.navigateToAnswer) {
            onNavigateToAnswer()
            viewModel.consumeAnswerNavigation()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Study Mentor - Ask AI") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("What would you like to learn or solve today?", style = MaterialTheme.typography.titleMedium)

            // Subject Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject") },
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

            // Prompt input
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                label = { Text("Ask your study question or paste problem text...") },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = viewModel::submitQuestion,
                    enabled = state.prompt.isNotBlank() && !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Solve")
                        Spacer(Modifier.width(8.dp))
                        Text("Solve Problem")
                    }
                }
            }

            if (state.isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBlock(Modifier.fillMaxWidth().height(22.dp))
                    ShimmerBlock(Modifier.fillMaxWidth().height(108.dp))
                    ShimmerBlock(Modifier.fillMaxWidth(0.75f).height(72.dp))
                }
            }
        }
    }
}
