package com.aistudymentor.ui.quizzes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizzesScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Quizzes") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ready to test your knowledge?", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { /* Start Quiz */ }) {
                Text("Start Calculus Practice Quiz")
            }
        }
    }
}
