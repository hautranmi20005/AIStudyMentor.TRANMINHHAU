package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.ui.common.ObserveSnackbarEvents
import com.aistudymentor.ui.common.ShimmerBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianConsentScreen(viewModel: AuthViewModel, onConsentApproved: () -> Unit, onLogout: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbarHostState)

    LaunchedEffect(Unit) { viewModel.loadGuardianConsentStatus() }
    LaunchedEffect(state.consentStatus?.status) {
        if (state.consentStatus?.status.equals("APPROVED", ignoreCase = true)) onConsentApproved()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Guardian Consent Required") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You are registered as under 13 years old. Per safety regulations, a parent or guardian must approve your account via the verification email sent to their address.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current Status:", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        when {
                            state.isLoading && state.consentStatus == null -> ShimmerBlock(Modifier.fillMaxWidth().height(20.dp))
                            state.consentStatus == null -> Text("Sign in again after your guardian approves the email.")
                            else -> Text(
                                "${state.consentStatus?.status ?: "PENDING"} · ${state.consentStatus?.guardianEmail.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = viewModel::loadGuardianConsentStatus,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Status")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::resendGuardianConsent,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resend Guardian Email")
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onLogout) {
                    Text("Log Out / Switch Account")
                }
            }
        }
    }
}
