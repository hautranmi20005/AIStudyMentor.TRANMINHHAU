package com.aistudymentor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudymentor.R
import com.aistudymentor.data.AppContainer
import com.aistudymentor.ui.ai.AnswerDetailScreen
import com.aistudymentor.ui.ai.AskAiScreen
import com.aistudymentor.ui.ai.AskAiViewModel
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.ui.auth.GuardianConsentScreen
import com.aistudymentor.ui.auth.LoginScreen
import com.aistudymentor.ui.auth.OnboardingScreen
import com.aistudymentor.ui.auth.RegisterScreen
import com.aistudymentor.ui.dashboard.DashboardScreen
import com.aistudymentor.ui.dashboard.DashboardViewModel
import com.aistudymentor.ui.history.HistoryScreen
import com.aistudymentor.ui.history.OfflineLibraryViewModel
import com.aistudymentor.ui.navigation.Screen
import com.aistudymentor.ui.profile.AboutScreen
import com.aistudymentor.ui.profile.PrivacyAccountScreen
import com.aistudymentor.ui.profile.ProfileScreen
import com.aistudymentor.ui.profile.NotificationSettingsScreen
import com.aistudymentor.ui.profile.NotificationSettingsViewModel
import com.aistudymentor.ui.quizzes.QuizViewModel
import com.aistudymentor.ui.quizzes.QuizzesScreen

private data class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainAppScreen(container: AppContainer, notificationRoute: String? = null) {
    val viewModelFactory = remember(container) { AppViewModelFactory(container) }
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val askAiViewModel: AskAiViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val offlineLibraryViewModel: OfflineLibraryViewModel = viewModel(factory = viewModelFactory)
    val askAiState by askAiViewModel.uiState.collectAsStateWithLifecycle()
    val offlineLibraryState by offlineLibraryViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var pendingRoute by rememberSaveable { mutableStateOf(notificationRoute) }

    LaunchedEffect(notificationRoute) {
        notificationRoute?.let { pendingRoute = it }
    }

    val items = listOf(
        BottomNavItem(Screen.AskAi.route, R.string.nav_ask, Icons.Default.Psychology),
        BottomNavItem(Screen.History.route, R.string.nav_history, Icons.Default.History),
        BottomNavItem(Screen.Quizzes.route, R.string.nav_quiz, Icons.Default.Quiz),
        BottomNavItem(Screen.Dashboard.route, R.string.nav_progress, Icons.Default.Dashboard),
        BottomNavItem(Screen.Profile.route, R.string.nav_profile, Icons.Default.Person)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = items.any { it.route == currentRoute }

    fun navigateToSignedInDestination(route: String) {
        val source = if (currentRoute == Screen.Onboarding.route) {
            Screen.Onboarding.route
        } else {
            Screen.AskAi.route
        }
        navController.navigate(route) {
            popUpTo(source) {
                inclusive = source == Screen.Onboarding.route
                saveState = source != Screen.Onboarding.route
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun finishOnboarding() {
        val route = pendingRoute ?: Screen.AskAi.route
        pendingRoute = null
        navigateToSignedInDestination(route)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        val title = stringResource(item.titleRes)
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.AskAi.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { requiresConsent ->
                        container.offlineSyncCoordinator.enqueueSync()
                        val destination = if (requiresConsent) {
                            Screen.GuardianConsent.route
                        } else {
                            Screen.Onboarding.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.GuardianConsent.route) {
                GuardianConsentScreen(
                    viewModel = authViewModel,
                    onConsentApproved = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.GuardianConsent.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(authViewModel, onComplete = ::finishOnboarding)
            }
            composable(Screen.AskAi.route) {
                LaunchedEffect(askAiViewModel) { askAiViewModel.refreshDailyQuota() }
                AskAiScreen(
                    viewModel = askAiViewModel,
                    recentQuestions = offlineLibraryState.questions.take(3),
                    isOnline = container.isOnline(),
                    onNavigateToAnswer = { questionId ->
                        if (questionId != null) {
                            navController.navigate(Screen.QuestionDetail.createRoute(questionId))
                        } else {
                            navController.navigate(Screen.AnswerDetail.route)
                        }
                    },
                    onOpenQuestion = { questionId ->
                        navController.navigate(Screen.QuestionDetail.createRoute(questionId))
                    },
                    onNavigateToQuizzes = { navigateToSignedInDestination(Screen.Quizzes.route) }
                )
            }
            composable(Screen.AnswerDetail.route) {
                AnswerDetailScreen(
                    explanation = askAiState.response?.answer?.structuredExplanation,
                    onBack = { navController.popBackStack() },
                    isDemo = askAiState.response?.answer?.isDemo == true
                )
            }
            composable(Screen.Quizzes.route) {
                val quizViewModel: QuizViewModel = viewModel(factory = viewModelFactory)
                QuizzesScreen(quizViewModel, isOnline = container.isOnline())
            }
            composable(Screen.Dashboard.route) { DashboardScreen(dashboardViewModel) }
            composable(Screen.History.route) {
                HistoryScreen(
                    offlineLibraryViewModel,
                    onOpenQuestion = { questionId ->
                        navController.navigate(Screen.QuestionDetail.createRoute(questionId))
                    }
                )
            }
            composable(
                route = Screen.QuestionDetail.route,
                arguments = listOf(navArgument("questionId") { type = NavType.StringType })
            ) { entry ->
                val questionId = entry.arguments?.getString("questionId")
                if (questionId != null) {
                    AnswerDetailScreen(
                        questionId = questionId,
                        viewModel = offlineLibraryViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onPrivacy = { navController.navigate(Screen.PrivacyAccount.route) },
                    onAbout = { navController.navigate(Screen.About.route) },
                    onNotifications = { navController.navigate(Screen.NotificationSettings.route) }
                )
            }
            composable(Screen.NotificationSettings.route) {
                val settingsViewModel: NotificationSettingsViewModel = viewModel(factory = viewModelFactory)
                NotificationSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PrivacyAccount.route) {
                PrivacyAccountScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    onLoggedOutAllDevices = {
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
