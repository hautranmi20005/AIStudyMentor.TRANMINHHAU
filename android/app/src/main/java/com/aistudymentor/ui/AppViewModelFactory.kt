package com.aistudymentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aistudymentor.data.AppContainer
import com.aistudymentor.ui.ai.AskAiViewModel
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.ui.dashboard.DashboardViewModel
import com.aistudymentor.ui.history.OfflineLibraryViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.authRepository)
        modelClass.isAssignableFrom(AskAiViewModel::class.java) -> AskAiViewModel(container.questionRepository)
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.dashboardRepository)
        modelClass.isAssignableFrom(OfflineLibraryViewModel::class.java) -> OfflineLibraryViewModel(container.offlineRepository)
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}