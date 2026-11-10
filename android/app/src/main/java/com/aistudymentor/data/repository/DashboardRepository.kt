package com.aistudymentor.data.repository

import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.security.SecureTokenStorage
import com.aistudymentor.data.telemetry.monitorRetrofitCall

class DashboardRepository(
    private val api: ApiService,
    private val tokenStorage: SecureTokenStorage
) {
    suspend fun metrics(): DashboardMetricsDto {
        val token = tokenStorage.accessToken()
            ?: throw ApiFailure(401, "AUTH_REQUIRED", "Please sign in to view your dashboard.", retryable = false)
        return monitorRetrofitCall("dashboard.metrics") {
            api.getDashboardMetrics("Bearer $token").requireBody()
        }
    }
}