package com.aistudymentor.ui.dashboard

import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.repository.DashboardRepository
import com.aistudymentor.ui.common.NetworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DashboardUiState(
    val metrics: DashboardMetricsDto? = null,
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false
)

class DashboardViewModel(private val repository: DashboardRepository) : NetworkViewModel() {
    private val mutableUiState = MutableStateFlow(DashboardUiState())
    val uiState = mutableUiState.asStateFlow()

    fun loadIfNeeded() {
        if (!mutableUiState.value.hasLoaded && !mutableUiState.value.isLoading) {
            loadMetrics()
        }
    }

    fun refresh() {
        loadMetrics()
    }

    private fun loadMetrics() {
        launchRequest(
            setLoading = { loading -> mutableUiState.update { it.copy(isLoading = loading) } },
            retry = { loadMetrics() },
            action = {
                val metrics = repository.metrics()
                mutableUiState.update { it.copy(metrics = metrics, hasLoaded = true) }
            }
        )
    }
}