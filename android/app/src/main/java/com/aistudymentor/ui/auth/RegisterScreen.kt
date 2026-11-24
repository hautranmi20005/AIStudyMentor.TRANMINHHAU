package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: (Boolean) -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    val age = state.ageText.toIntOrNull() ?: 16
    val isUnder13 = age < 13

    LaunchedEffect(state.registrationSucceeded) {
        if (state.registrationSucceeded) {
            onRegisterSuccess(state.guardianConsentRequired)
            viewModel.consumeRegistrationSuccess()
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
                    title = { Text(stringResource(R.string.register_title)) },
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
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.ageText,
                    onValueChange = viewModel::updateAge,
                    label = { Text(stringResource(R.string.age)) },
                    keyboardOptions = KeyboardOptions.Default,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isUnder13) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.guardianEmail,
                        onValueChange = viewModel::updateGuardianEmail,
                        label = { Text(stringResource(R.string.guardian_email_required)) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.guardian_approval_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::register,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.register_account))
                }
                TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                    Text(stringResource(R.string.login_prompt))
                }
            }
        }
    }
}
