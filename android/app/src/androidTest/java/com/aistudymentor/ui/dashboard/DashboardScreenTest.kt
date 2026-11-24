package com.aistudymentor.ui.dashboard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudymentor.data.remote.ApiService
import com.aistudymentor.data.remote.DashboardMetricsDto
import com.aistudymentor.data.remote.SubjectBreakdownDto
import com.aistudymentor.data.repository.DashboardRepository
import com.aistudymentor.data.security.SecureTokenStorage
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var tokenStorage: SecureTokenStorage

    @Before
    fun setUp() {
        tokenStorage = SecureTokenStorage(ApplicationProvider.getApplicationContext<Context>())
        tokenStorage.saveSession("dashboard-access", "dashboard-refresh", "student-id")
    }

    @After
    fun tearDown() {
        tokenStorage.clear()
    }

    @Test
    fun loadedDashboardShowsMetricsAndSubjectBreakdown() {
        val metrics = DashboardMetricsDto(
            totalQuestionsAsked = 6,
            totalQuizzesCompleted = 2,
            overallAccuracyRate = 60.0,
            subjectBreakdown = listOf(SubjectBreakdownDto("Physics", 4, 2, 40.0))
        )
        val viewModel = DashboardViewModel(DashboardRepository(apiReturning(metrics), tokenStorage))
        composeRule.setContent { MaterialTheme { DashboardScreen(viewModel) } }

        composeRule.waitUntil(5_000) { viewModel.uiState.value.metrics != null }
        composeRule.onNodeWithText("Questions asked").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("Physics").assertIsDisplayed()
        composeRule.onNodeWithText("4 questions · 2 quizzes").assertIsDisplayed()
    }

    @Test
    fun dashboardFailureShowsRetryState() {
        val api = apiReturningFailure()
        val viewModel = DashboardViewModel(DashboardRepository(api, tokenStorage))
        composeRule.setContent { MaterialTheme { DashboardScreen(viewModel) } }

        composeRule.waitUntil(5_000) { viewModel.uiState.value.hasLoaded }
        composeRule.onNodeWithText("Progress is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun apiReturning(metrics: DashboardMetricsDto): ApiService =
        apiProxy { method, _ ->
            if (method == "getDashboardMetrics") Response.success(metrics)
            else error("Unexpected API call: $method")
        }

    private fun apiReturningFailure(): ApiService =
        apiProxy { method, _ ->
            if (method == "getDashboardMetrics") error("Synthetic dashboard failure")
            else error("Unexpected API call: $method")
        }

    private fun apiProxy(response: (String, Array<out Any?>) -> Any?): ApiService =
        Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args -> response(method.name, args ?: emptyArray()) } as ApiService
}
