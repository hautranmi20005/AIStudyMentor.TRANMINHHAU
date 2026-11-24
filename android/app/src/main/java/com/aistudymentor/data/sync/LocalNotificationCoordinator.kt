package com.aistudymentor.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class LocalNotificationCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun scheduleActiveAccount(accountId: String?) {
        if (accountId.isNullOrBlank()) return
        val input = workDataOf(LocalNotificationWorker.ACCOUNT_ID to accountId)
        val periodic = PeriodicWorkRequestBuilder<LocalNotificationWorker>(24, TimeUnit.HOURS)
            .setInputData(input)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            periodicName(accountId), ExistingPeriodicWorkPolicy.UPDATE, periodic
        )
        val immediate = OneTimeWorkRequestBuilder<LocalNotificationWorker>()
            .setInputData(input)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(immediateName(accountId), ExistingWorkPolicy.KEEP, immediate)
    }

    fun reevaluate(accountId: String?) = scheduleActiveAccount(accountId)

    suspend fun clearQueuedWork() = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(TAG).result.get()
    }

    private fun periodicName(accountId: String) = "p13-local-notifications-$accountId"
    private fun immediateName(accountId: String) = "p13-local-notifications-now-$accountId"

    private companion object {
        const val TAG = "p13-local-notifications"
    }
}
