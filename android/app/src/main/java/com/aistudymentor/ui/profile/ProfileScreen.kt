package com.aistudymentor.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistudymentor.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onPrivacy: () -> Unit = {},
    onAbout: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Text(stringResource(R.string.profile_notifications))
            }
            OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null)
                Text(stringResource(R.string.profile_privacy))
            }
            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text(stringResource(R.string.profile_about))
            }
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.logout))
            }
        }
    }
}
