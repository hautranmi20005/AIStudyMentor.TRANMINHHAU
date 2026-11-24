package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
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
                    title = { Text(stringResource(R.string.guardian_title)) },
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
                    stringResource(R.string.guardian_description),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.guardian_current_status), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        when {
                            state.isLoading && state.consentStatus == null -> ShimmerBlock(Modifier.fillMaxWidth().height(20.dp))
                            state.consentStatus == null -> Text(stringResource(R.string.guardian_sign_in_after_approval))
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
                    Text(stringResource(R.string.guardian_check_status))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::resendGuardianConsent,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.guardian_resend_email))
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onLogout) {
                    Text(stringResource(R.string.logout_switch_account))
                }
            }
        }
    }
}
