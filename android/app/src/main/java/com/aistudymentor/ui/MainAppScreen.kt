package com.aistudymentor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.aistudymentor.data.AppContainer
import com.aistudymentor.ui.AppViewModelFactory
import com.aistudymentor.ui.ai.AnswerDetailScreen
import com.aistudymentor.ui.ai.AskAiScreen
import com.aistudymentor.ui.ai.AskAiViewModel
import com.aistudymentor.ui.auth.AuthViewModel
import com.aistudymentor.ui.auth.GuardianConsentScreen
import com.aistudymentor.ui.auth.LoginScreen
import com.aistudymentor.ui.auth.RegisterScreen
import com.aistudymentor.ui.dashboard.DashboardScreen
import com.aistudymentor.ui.dashboard.DashboardViewModel
import com.aistudymentor.ui.history.HistoryScreen
import com.aistudymentor.ui.history.OfflineLibraryViewModel
import com.aistudymentor.ui.navigation.Screen
import com.aistudymentor.ui.profile.ProfileScreen
import com.aistudymentor.ui.quizzes.QuizzesScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object AskAi : BottomNavItem(Screen.AskAi.route, "Ask AI", Icons.Default.Psychology)
    object Quizzes : BottomNavItem(Screen.Quizzes.route, "Quizzes", Icons.Default.Quiz)
    object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard)
    object History : BottomNavItem(Screen.History.route, "History", Icons.Default.History)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(container: AppContainer) {
    val viewModelFactory = remember(container) { AppViewModelFactory(container) }
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val askAiViewModel: AskAiViewModel = viewModel(factory = viewModelFactory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val offlineLibraryViewModel: OfflineLibraryViewModel = viewModel(factory = viewModelFactory)
    val askAiState by askAiViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.AskAi,
        BottomNavItem.Quizzes,
        BottomNavItem.Dashboard,
        BottomNavItem.History,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in items.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
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
                        navController.navigate(if (requiresConsent) Screen.GuardianConsent.route else Screen.AskAi.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
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
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.GuardianConsent.route) {
                GuardianConsentScreen(
                    viewModel = authViewModel,
                    onConsentApproved = {
                        navController.navigate(Screen.AskAi.route) {
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
            composable(Screen.AskAi.route) {
                AskAiScreen(
                    viewModel = askAiViewModel,
                    onNavigateToAnswer = {
                        navController.navigate(Screen.AnswerDetail.route)
                    }
                )
            }
            composable(Screen.AnswerDetail.route) {
                AnswerDetailScreen(
                    explanation = askAiState.response?.answer?.structuredExplanation,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Quizzes.route) {
                QuizzesScreen()
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(dashboardViewModel)
            }
            composable(Screen.History.route) {
                HistoryScreen(offlineLibraryViewModel)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
