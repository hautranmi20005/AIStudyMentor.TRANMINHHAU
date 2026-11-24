package com.aistudymentor

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aistudymentor.ui.MainAppScreen
import com.aistudymentor.ui.navigation.notificationRouteForHost
import com.aistudymentor.ui.theme.StudyMentorTheme

class MainActivity : ComponentActivity() {
    private var notificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationRoute = notificationRouteForHost(intent?.data?.host)
        val container = (application as StudyMentorApplication).appContainer
        setContent {
            StudyMentorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(container, notificationRoute)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute = notificationRouteForHost(intent.data?.host)
    }
}
