package com.aistudymentor.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.NetworkModule
import com.aistudymentor.data.repository.AuthRepository
import com.aistudymentor.data.repository.ApiQuizRepository
import com.aistudymentor.data.repository.DashboardRepository
import com.aistudymentor.data.repository.NotificationSettingsRepository
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.sync.LocalNotificationCoordinator
import com.aistudymentor.data.sync.OfflineSyncCoordinator

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStorage = SecureTokenStorage(context)
    private val apiService = NetworkModule.createService<com.aistudymentor.data.remote.ApiService>()
    private val database = StudyMentorDatabase.getInstance(appContext)
    private val offlineDao = database.offlineDao()
    private val localNotificationCoordinator = LocalNotificationCoordinator(appContext)

    val offlineSyncCoordinator = OfflineSyncCoordinator(appContext)
    val authRepository = AuthRepository(apiService, tokenStorage)
    val offlineRepository = OfflineRepository(apiService, database, offlineDao, tokenStorage)
    val questionRepository = QuestionRepository(apiService, tokenStorage, offlineRepository)
    val dashboardRepository = DashboardRepository(apiService, tokenStorage)
    val quizRepository = ApiQuizRepository(apiService, tokenStorage)
    val notificationSettingsRepository = NotificationSettingsRepository(
        apiService,
        tokenStorage,
        offlineRepository,
        ::isOnline,
        { localNotificationCoordinator.reevaluate(tokenStorage.userId()) }
    )

    fun currentAccountId(): String? = tokenStorage.userId()

    fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}