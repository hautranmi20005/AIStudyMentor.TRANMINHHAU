package com.aistudymentor.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.aistudymentor.R

@Composable
fun RecoveryCodesScreen(viewModel: AuthViewModel, onContinue: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.recovery_codes_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.recovery_codes_description))
            Spacer(Modifier.height(16.dp))
            state.recoveryCodes.forEach { code ->
                Text(code, style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(state.recoveryCodes.joinToString("\n")))
            }) {
                Text(stringResource(R.string.copy_recovery_codes))
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                viewModel.consumeRecoveryCodes()
                onContinue()
            }) {
                Text(stringResource(R.string.recovery_codes_saved))
            }
        }
    }
}
