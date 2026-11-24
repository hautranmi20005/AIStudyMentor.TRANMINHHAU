package com.aistudymentor.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudymentor.R
import com.aistudymentor.data.local.NotificationPreferencesEntity
import com.aistudymentor.ui.common.ErrorState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = state.preferences ?: NotificationPreferencesEntity(accountId = "")
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permissionDeniedMessage = stringResource(R.string.notification_permission_denied)
    var pendingEnable by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingEnable
        pendingEnable = null
        if (granted) {
            when (target) {
                ALL -> viewModel.setAllEnabled(true)
                NotificationSettingsViewModel.BOOKMARK_REVIEW ->
                    viewModel.setEnabled(NotificationSettingsViewModel.BOOKMARK_REVIEW, true)
                NotificationSettingsViewModel.REPEATED_MISTAKE ->
                    viewModel.setEnabled(NotificationSettingsViewModel.REPEATED_MISTAKE, true)
                NotificationSettingsViewModel.WEEKLY_SUMMARY ->
                    viewModel.setEnabled(NotificationSettingsViewModel.WEEKLY_SUMMARY, true)
            }
        } else {
            scope.launch { snackbarHost.showSnackbar(permissionDeniedMessage) }
        }
    }

    fun updateEnabled(type: String, enabled: Boolean) {
        val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (enabled && permissionMissing) {
            pendingEnable = type
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (type == ALL) {
            viewModel.setAllEnabled(enabled)
        } else {
            viewModel.setEnabled(type, enabled)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.notification_settings_description),
                style = MaterialTheme.typography.bodyLarge
            )
            if (state.loading) {
                CircularProgressIndicator()
            }
            state.error?.let {
                ErrorState(messageRes = R.string.notification_settings_error, onRetry = viewModel::refresh)
            }
            val anyEnabled = preferences.bookmarkReviewEnabled ||
                preferences.repeatedMistakeEnabled || preferences.weeklySummaryEnabled
            SettingsToggle(
                title = stringResource(R.string.notification_master),
                checked = anyEnabled,
                onCheckedChange = { updateEnabled(ALL, it) }
            )
            SettingsToggle(
                title = stringResource(R.string.notification_bookmarks),
                checked = preferences.bookmarkReviewEnabled,
                onCheckedChange = { updateEnabled(NotificationSettingsViewModel.BOOKMARK_REVIEW, it) }
            )
            SettingsToggle(
                title = stringResource(R.string.notification_mistakes),
                checked = preferences.repeatedMistakeEnabled,
                onCheckedChange = { updateEnabled(NotificationSettingsViewModel.REPEATED_MISTAKE, it) }
            )
            SettingsToggle(
                title = stringResource(R.string.notification_summary),
                checked = preferences.weeklySummaryEnabled,
                onCheckedChange = { updateEnabled(NotificationSettingsViewModel.WEEKLY_SUMMARY, it) }
            )
        }
    }
}

@Composable
private fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private const val ALL = "ALL"
