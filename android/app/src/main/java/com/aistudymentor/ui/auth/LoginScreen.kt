package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (Boolean) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onLoginSuccess(state.guardianConsentRequired)
            viewModel.consumeAuthNavigation()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.login_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.email)) },
                    enabled = !state.isLoading && !state.mfaRequired,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                if (state.mfaRequired) {
                    OutlinedTextField(
                        value = state.mfaCode,
                        onValueChange = viewModel::updateMfaCode,
                        label = { Text(stringResource(R.string.mfa_code_hint)) },
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    )
                } else {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isLoading,
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                )
                }
                state.registrationNoticeRes?.let { resource ->
                    Spacer(Modifier.height(16.dp))
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(resource, *state.registrationNoticeArgs.toTypedArray()),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { if (state.mfaRequired) viewModel.verifyMfa() else viewModel.login() },
                    enabled = !state.isLoading,
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (state.mfaRequired) R.string.verify_code else R.string.sign_in))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (!state.mfaRequired) {
                    TextButton(onClick = onNavigateToRegister, enabled = !state.isLoading) {
                        Text(stringResource(R.string.register_prompt))
                    }
                }
            }
        }
    }
}
