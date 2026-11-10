package com.aistudymentor.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudymentor.StudyMentorApplication
import com.aistudymentor.data.repository.ApiFailure
import com.aistudymentor.data.repository.isRetryableNetworkFailure

class OfflineSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as StudyMentorApplication).appContainer
            container.offlineRepository.syncFromRemote()
            Result.success()
        } catch (failure: Exception) {
            when {
                failure.isRetryableNetworkFailure() -> Result.retry()
                (failure as? ApiFailure)?.statusCode == 401 -> Result.success()
                else -> Result.failure()
            }
        }
    }
}