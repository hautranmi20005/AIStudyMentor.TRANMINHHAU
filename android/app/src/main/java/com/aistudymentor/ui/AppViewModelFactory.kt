package com.aistudymentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.aistudymentor.data.AppContainer
import com.aistudymentor.ui.ai.AskAiViewModel
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.ui.dashboard.DashboardViewModel
import com.aistudymentor.ui.history.OfflineLibraryViewModel
import com.aistudymentor.ui.profile.NotificationSettingsViewModel
import com.aistudymentor.ui.quizzes.QuizViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = createViewModel(modelClass, null)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        createViewModel(modelClass, extras)

    private fun <T : ViewModel> createViewModel(modelClass: Class<T>, extras: CreationExtras?): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.authRepository)
        modelClass.isAssignableFrom(AskAiViewModel::class.java) -> AskAiViewModel(container.questionRepository)
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.dashboardRepository)
        modelClass.isAssignableFrom(OfflineLibraryViewModel::class.java) ->
            OfflineLibraryViewModel(container.offlineRepository, container.questionRepository, container::isOnline)
        modelClass.isAssignableFrom(QuizViewModel::class.java) -> QuizViewModel(
            container.quizRepository,
            extras?.createSavedStateHandle() ?: SavedStateHandle(),
            container.isOnline()
        )
        modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java) ->
            NotificationSettingsViewModel(container.notificationSettingsRepository)
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    } as T
}