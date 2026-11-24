package com.aistudymentor.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.R
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAccountScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onLoggedOutAllDevices: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var password by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    ObserveSnackbarEvents(viewModel, snackbar)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.privacy_title)) }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.privacy_data_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.privacy_data_details)
            )
            Text(
                stringResource(R.string.privacy_delete_details),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reauthenticate), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("privacy-password")
            )
            OutlinedTextField(
                value = mfaCode,
                onValueChange = { mfaCode = it.take(20) },
                label = { Text(stringResource(R.string.authenticator_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("privacy-mfa-code")
            )
            Button(
                onClick = {
                    viewModel.logoutAllDevices(password, mfaCode, onLoggedOutAllDevices)
                },
                enabled = password.isNotBlank() && mfaCode.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("privacy-logout-all-button")
            ) {
                Text(stringResource(R.string.logout_all_devices))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.delete_account_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.delete_confirmation_hint))
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = { Text(stringResource(R.string.delete_confirmation_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("privacy-delete-confirmation")
            )
            Button(
                onClick = {
                    viewModel.deleteAccount(password, mfaCode, onDeleted)
                },
                enabled = confirmation == stringResource(R.string.delete_confirmation_phrase)
                    && password.isNotBlank()
                    && mfaCode.isNotBlank()
                    && !state.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("privacy-delete-button")
            ) {
                if (state.isLoading) CircularProgressIndicator()
                else Text(stringResource(R.string.delete_account))
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back_to_profile))
            }
        }
    }
}
