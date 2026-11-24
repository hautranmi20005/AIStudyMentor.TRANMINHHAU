package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@Composable
fun OnboardingScreen(viewModel: AuthViewModel, onComplete: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(0) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbar)
    LaunchedEffect(viewModel) { viewModel.loadProfile() }
    LaunchedEffect(state.profile?.onboardingCompleted) {
        if (state.profile?.onboardingCompleted == true) onComplete()
    }
    Scaffold(snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.onboarding_step, step + 1),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> OutlinedTextField(
                    state.educationLevel,
                    viewModel::updateEducationLevel,
                    label = { Text(stringResource(R.string.education_level)) },
                    modifier = Modifier.fillMaxWidth()
                )
                else -> OutlinedTextField(
                    state.explanationStyle,
                    viewModel::updateExplanationStyle,
                    label = { Text(stringResource(R.string.explanation_style)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (step < 1) step++ else viewModel.saveProfile()
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (step < 1) R.string.next else R.string.finish_onboarding))
            }
        }
    }
}
