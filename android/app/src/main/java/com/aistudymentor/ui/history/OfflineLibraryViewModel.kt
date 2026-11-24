package com.aistudymentor.ui.history

import com.aistudymentor.R
import androidx.lifecycle.viewModelScope
import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

data class OfflineLibraryUiState(
    val questions: List<CachedQuestionEntity> = emptyList(),
    val bookmarks: List<CachedBookmarkEntity> = emptyList(),
    val quizResults: List<CachedQuizResultEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val detail: CachedQuestionEntity? = null,
    val isDetailLoading: Boolean = false,
    val isBookmarkSaving: Boolean = false,
    val isOnline: Boolean = true,
    val pendingBookmarkCount: Int = 0
)

private data class OfflineLibraryData(
    val questions: List<CachedQuestionEntity>,
    val bookmarks: List<CachedBookmarkEntity>,
    val quizResults: List<CachedQuizResultEntity>,
    val isSyncing: Boolean,
    val pendingBookmarkCount: Int
)

class OfflineLibraryViewModel(
    private val repository: OfflineRepository,
    private val questionRepository: QuestionRepository,
    private val online: () -> Boolean
) : NetworkViewModel() {
    private val isSyncing = MutableStateFlow(false)
    private val mutableUiState = MutableStateFlow(OfflineLibraryUiState())
    val uiState = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.questionHistory(),
                repository.bookmarks(),
                repository.quizResults(),
                repository.pendingBookmarkCount(),
                isSyncing
            ) { questions, bookmarks, results, pendingCount, syncing ->
                OfflineLibraryData(questions, bookmarks, results, syncing, pendingCount)
            }.collect { data ->
                mutableUiState.update {
                    it.copy(
                        questions = data.questions,
                        bookmarks = data.bookmarks,
                        quizResults = data.quizResults,
                        isSyncing = data.isSyncing,
                        pendingBookmarkCount = data.pendingBookmarkCount
                    )
                }
            }
        }
    }

    fun synchronize() {
        launchRequest(
            setLoading = { isSyncing.value = it },
            retry = { synchronize() },
            action = { repository.syncFromRemote() }
        )
    }

    fun openQuestion(questionId: String) {
        val isOnline = online()
        launchRequest(
            setLoading = { loading ->
                mutableUiState.update {
                    it.copy(
                        isDetailLoading = loading,
                        isOnline = isOnline,
                        detail = if (loading) null else it.detail
                    )
                }
            },
            retry = { openQuestion(questionId) },
            action = {
                if (isOnline) questionRepository.getQuestion(questionId)
                val detail = repository.cachedQuestion(questionId)
                    ?: throw IllegalStateException("This question is not available in the offline library.")
                mutableUiState.update { it.copy(detail = detail) }
            }
        )
    }

    fun setBookmark(questionId: String, bookmarked: Boolean) {
        val isOnline = online()
        if (!isOnline) {
            emitSnackbar(R.string.offline_bookmark_reconnect)
            return
        }
        launchRequest(
            setLoading = { loading ->
                mutableUiState.update { it.copy(isBookmarkSaving = loading, isOnline = true) }
            },
            retry = { setBookmark(questionId, bookmarked) },
            action = {
                questionRepository.setBookmark(questionId, bookmarked)
                val detail = repository.cachedQuestion(questionId)
                mutableUiState.update { it.copy(detail = detail) }
            }
        )
    }
}
