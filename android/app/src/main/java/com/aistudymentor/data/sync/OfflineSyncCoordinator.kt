package com.aistudymentor.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class OfflineSyncCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val workManager = WorkManager.getInstance(appContext)

    fun start() {
        enqueueSync()
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    enqueueSync()
                }
            })
        }
    }

    fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(OFFLINE_SYNC_TAG)
            .build()
        workManager.enqueueUniqueWork(OFFLINE_SYNC_WORK, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val OFFLINE_SYNC_WORK = "offline-history-sync"
        const val OFFLINE_SYNC_TAG = "offline-history"
    }
}