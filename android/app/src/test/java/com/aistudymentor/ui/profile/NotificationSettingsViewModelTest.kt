package com.aistudymentor.ui.profile

import com.aistudymentor.data.local.NotificationPreferencesEntity
import com.aistudymentor.data.repository.NotificationPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatesArePersistedLocallyAndReflectedThroughThePreferenceSource() = runTest(dispatcher) {
        val source = FakeNotificationPreferencesSource()
        val viewModel = NotificationSettingsViewModel(source)
        advanceUntilIdle()

        viewModel.setEnabled(NotificationSettingsViewModel.BOOKMARK_REVIEW, false)
        advanceUntilIdle()

        assertFalse(source.current.value?.bookmarkReviewEnabled ?: true)
        assertTrue(source.current.value?.pendingUpload ?: false)
    }

    @Test
    fun masterTogglePersistsAllReminderCategoriesTogether() = runTest(dispatcher) {
        val source = FakeNotificationPreferencesSource()
        val viewModel = NotificationSettingsViewModel(source)
        advanceUntilIdle()

        viewModel.setAllEnabled(false)
        advanceUntilIdle()

        val preferences = source.current.value
        assertFalse(preferences?.bookmarkReviewEnabled ?: true)
        assertFalse(preferences?.repeatedMistakeEnabled ?: true)
        assertFalse(preferences?.weeklySummaryEnabled ?: true)
    }

    private class FakeNotificationPreferencesSource : NotificationPreferencesSource {
        val current = MutableStateFlow<NotificationPreferencesEntity?>(
            NotificationPreferencesEntity(accountId = "account")
        )

        override fun observe(): Flow<NotificationPreferencesEntity?> = current
        override suspend fun refreshFromServer() = Unit
        override suspend fun update(preferences: NotificationPreferencesEntity) {
            current.value = preferences.copy(accountId = "account", pendingUpload = true)
        }
    }
}
