package com.aistudymentor.ui.history

import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OfflineLibraryUiState(
    val questions: List<CachedQuestionEntity> = emptyList(),
    val bookmarks: List<CachedBookmarkEntity> = emptyList(),
    val quizResults: List<CachedQuizResultEntity> = emptyList(),
    val isSyncing: Boolean = false
)

class OfflineLibraryViewModel(private val repository: OfflineRepository) : NetworkViewModel() {
    private val isSyncing = MutableStateFlow(false)
    val uiState = combine(
        repository.questionHistory(),
        repository.bookmarks(),
        repository.quizResults(),
        isSyncing
    ) { questions, bookmarks, results, syncing ->
        OfflineLibraryUiState(questions, bookmarks, results, syncing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfflineLibraryUiState())

    fun synchronize() {
        launchRequest(
            setLoading = { isSyncing.value = it },
            retry = { synchronize() },
            action = { repository.syncFromRemote() }
        )
    }
}
