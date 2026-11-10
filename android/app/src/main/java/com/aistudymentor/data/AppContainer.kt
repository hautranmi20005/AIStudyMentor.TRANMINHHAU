package com.aistudymentor.data

import android.content.Context
import com.aistudymentor.data.local.StudyMentorDatabase
import com.aistudymentor.data.remote.NetworkModule
import com.aistudymentor.data.repository.AuthRepository
import com.aistudymentor.data.repository.DashboardRepository
import com.aistudymentor.data.repository.OfflineRepository
import com.aistudymentor.data.repository.QuestionRepository
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.sync.OfflineSyncCoordinator

class AppContainer(context: Context) {
    private val tokenStorage = SecureTokenStorage(context)
    private val apiService = NetworkModule.createService<com.aistudymentor.data.remote.ApiService>()
    private val database = StudyMentorDatabase.getInstance(context)
    private val offlineDao = database.offlineDao()

    val offlineSyncCoordinator = OfflineSyncCoordinator(context)
    val authRepository = AuthRepository(apiService, tokenStorage)
    val offlineRepository = OfflineRepository(apiService, database, offlineDao, tokenStorage)
    val questionRepository = QuestionRepository(apiService, tokenStorage, offlineRepository)
    val dashboardRepository = DashboardRepository(apiService, tokenStorage)
}