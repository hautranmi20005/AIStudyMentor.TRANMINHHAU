package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    title = { Text("Register - AI Study Mentor") },
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
                    label = { Text("Email") },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.ageText,
                    onValueChange = viewModel::updateAge,
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions.Default,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isUnder13) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.guardianEmail,
                        onValueChange = viewModel::updateGuardianEmail,
                        label = { Text("Guardian Email (Required for under 13)") },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A parent or guardian must verify your account before you can ask study questions.",
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
                    else Text("Register Account")
                }
                TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                    Text("Already have an account? Login")
                }
            }
        }
    }
}
