package com.aistudymentor.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object GuardianConsent : Screen("guardian_consent")
    object Onboarding : Screen("onboarding")
    object AskAi : Screen("ask_ai")
    object AnswerDetail : Screen("answer_detail")
    object Quizzes : Screen("quizzes")
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Profile : Screen("profile")
    object PrivacyAccount : Screen("privacy_account")
    object About : Screen("about")
    object NotificationSettings : Screen("notification_settings")
    object QuestionDetail : Screen("question_detail/{questionId}") {
        fun createRoute(questionId: String): String = "question_detail/${Uri.encode(questionId)}"
    }
}
