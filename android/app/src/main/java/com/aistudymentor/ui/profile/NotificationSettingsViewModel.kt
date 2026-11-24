package com.aistudymentor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.local.NotificationPreferencesEntity
import com.aistudymentor.data.repository.NotificationPreferencesSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val preferences: NotificationPreferencesEntity? = null,
    val loading: Boolean = true,
    val error: String? = null
)

class NotificationSettingsViewModel(
    private val source: NotificationPreferencesSource
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(source.observe(), loading, error) { preferences, isLoading, message ->
        NotificationSettingsUiState(preferences, isLoading, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettingsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            try {
                source.refreshFromServer()
                error.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error.value = failure.message ?: "Notification settings could not be refreshed."
            } finally {
                loading.value = false
            }
        }
    }

    fun setEnabled(type: String, enabled: Boolean) {
        val current = uiState.value.preferences ?: NotificationPreferencesEntity(accountId = "")
        val updated = when (type) {
            BOOKMARK_REVIEW -> current.copy(bookmarkReviewEnabled = enabled)
            REPEATED_MISTAKE -> current.copy(repeatedMistakeEnabled = enabled)
            WEEKLY_SUMMARY -> current.copy(weeklySummaryEnabled = enabled)
            else -> return
        }
        save(updated)
    }

    fun setAllEnabled(enabled: Boolean) {
        val current = uiState.value.preferences ?: NotificationPreferencesEntity(accountId = "")
        save(
            current.copy(
                bookmarkReviewEnabled = enabled,
                repeatedMistakeEnabled = enabled,
                weeklySummaryEnabled = enabled
            )
        )
    }

    private fun save(updated: NotificationPreferencesEntity) {
        viewModelScope.launch {
            try {
                source.update(updated)
                error.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error.value = failure.message ?: "Notification settings could not be saved."
            }
        }
    }

    companion object {
        const val BOOKMARK_REVIEW = "BOOKMARK_REVIEW"
        const val REPEATED_MISTAKE = "REPEATED_MISTAKE"
        const val WEEKLY_SUMMARY = "WEEKLY_SUMMARY"
    }
}
