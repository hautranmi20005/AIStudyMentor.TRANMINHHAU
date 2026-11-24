package com.aistudymentor.ui.dashboard

import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.repository.DashboardDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @Test
    fun loadsMetricsOnceUntilExplicitRefresh() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeDashboardDataSource()
            val viewModel = DashboardViewModel(repository)

            viewModel.loadIfNeeded()
            advanceUntilIdle()
            viewModel.loadIfNeeded()
            advanceUntilIdle()

            assertEquals(1, repository.loadCount)
            assertEquals(12L, viewModel.uiState.value.metrics?.totalQuestionsAsked)
            assertTrue(viewModel.uiState.value.hasLoaded)

            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(2, repository.loadCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeDashboardDataSource : DashboardDataSource {
        var loadCount = 0

        override suspend fun metrics(): DashboardMetricsDto {
            loadCount++
            return DashboardMetricsDto(12, 3, 75.0, emptyList())
        }
    }
}
