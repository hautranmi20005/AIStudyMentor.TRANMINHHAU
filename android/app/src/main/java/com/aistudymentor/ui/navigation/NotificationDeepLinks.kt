package com.aistudymentor.ui.navigation

fun notificationRouteForHost(host: String?): String? = when (host?.lowercase()) {
    "bookmarks" -> Screen.History.route
    "quizzes" -> Screen.Quizzes.route
    "dashboard" -> Screen.Dashboard.route
    else -> null
}
