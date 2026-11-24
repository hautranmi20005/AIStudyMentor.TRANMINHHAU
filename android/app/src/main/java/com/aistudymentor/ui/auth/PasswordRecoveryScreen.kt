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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.ui.common.ObserveSnackbarEvents

@Composable
fun PasswordRecoveryScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    ObserveSnackbarEvents(viewModel, snackbar)
    Scaffold(snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.password_recovery_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                state.forgotPasswordEmail,
                viewModel::updateForgotPasswordEmail,
                label = { Text(stringResource(R.string.account_email)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = viewModel::requestPasswordReset, enabled = !state.isLoading) {
                Text(stringResource(R.string.send_reset_email))
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                state.resetToken,
                viewModel::updateResetToken,
                label = { Text(stringResource(R.string.reset_token)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                state.newPassword,
                viewModel::updateNewPassword,
                label = { Text(stringResource(R.string.new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = viewModel::resetPassword, enabled = !state.isLoading) {
                Text(stringResource(R.string.reset_password))
            }
            androidx.compose.material3.TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_sign_in)) }
        }
    }
}
