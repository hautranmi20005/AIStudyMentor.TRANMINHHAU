package com.aistudymentor.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object GuardianConsent : Screen("guardian_consent")
    object AskAi : Screen("ask_ai")
    object AnswerDetail : Screen("answer_detail")
    object Quizzes : Screen("quizzes")
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Profile : Screen("profile")
}
