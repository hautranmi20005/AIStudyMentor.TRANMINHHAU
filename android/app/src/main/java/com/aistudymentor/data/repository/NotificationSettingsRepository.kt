package com.aistudymentor.data.repository

import com.aistudymentor.data.local.NotificationPreferencesEntity
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.NotificationPreferencesDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall
import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesSource {
    fun observe(): Flow<NotificationPreferencesEntity?>
    suspend fun refreshFromServer()
    suspend fun update(preferences: NotificationPreferencesEntity)
}

class NotificationSettingsRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage,
    private val offlineRepository: OfflineRepository,
    private val isOnline: () -> Boolean,
    private val scheduleEvaluation: () -> Unit
) : NotificationPreferencesSource {

    override fun observe(): Flow<NotificationPreferencesEntity?> = offlineRepository.notificationPreferences()

    override suspend fun refreshFromServer() {
        val accountId = tokenStorage.userId() ?: return
        val current = offlineRepository.notificationPreferences(accountId)
        if (!isOnline()) {
            if (current != null) scheduleEvaluation()
            return
        }
        if (current?.pendingUpload == true) {
            syncPending(accountId, current)
            return
        }
        val accessToken = tokenStorage.accessToken() ?: return
        val remote = monitorRetrofitCall("notifications.preferences.load") {
            api.getNotificationPreferences("Bearer $accessToken").requireBody()
        }
        offlineRepository.saveNotificationPreferences(remote.toEntity(accountId, pendingUpload = false))
        scheduleEvaluation()
    }

    override suspend fun update(preferences: NotificationPreferencesEntity) {
        val accountId = tokenStorage.userId()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to update notification settings.", retryable = false)
        val pending = preferences.copy(accountId = accountId, pendingUpload = true)
        offlineRepository.saveNotificationPreferences(pending)
        scheduleEvaluation()
        if (isOnline()) syncPending(accountId, pending)
    }

    suspend fun syncPendingPreferences() {
        val accountId = tokenStorage.userId() ?: return
        val pending = offlineRepository.notificationPreferences(accountId) ?: return
        if (pending.pendingUpload && isOnline()) syncPending(accountId, pending)
    }

    private suspend fun syncPending(accountId: String, pending: NotificationPreferencesEntity) {
        val accessToken = tokenStorage.accessToken() ?: return
        val remote = monitorRetrofitCall("notifications.preferences.save") {
            api.updateNotificationPreferences("Bearer $accessToken", pending.toDto()).requireBody()
        }
        offlineRepository.saveNotificationPreferences(remote.toEntity(accountId, pendingUpload = false))
        offlineRepository.markNotificationPreferencesSynced(accountId)
    }

    private fun NotificationPreferencesEntity.toDto() = NotificationPreferencesDto(
        bookmarkReviewEnabled = bookmarkReviewEnabled,
        repeatedMistakeEnabled = repeatedMistakeEnabled,
        weeklySummaryEnabled = weeklySummaryEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        timezone = timezone
    )

    private fun NotificationPreferencesDto.toEntity(
        accountId: String,
        pendingUpload: Boolean
    ) = NotificationPreferencesEntity(
        accountId = accountId,
        bookmarkReviewEnabled = bookmarkReviewEnabled,
        repeatedMistakeEnabled = repeatedMistakeEnabled,
        weeklySummaryEnabled = weeklySummaryEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        timezone = timezone,
        pendingUpload = pendingUpload
    )
}
