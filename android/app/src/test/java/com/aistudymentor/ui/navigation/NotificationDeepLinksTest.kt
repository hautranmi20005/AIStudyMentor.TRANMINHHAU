package com.aistudymentor.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationDeepLinksTest {
    @Test
    fun notificationRoutesMapToTheExpectedScreens() {
        assertEquals(Screen.History.route, notificationRouteForHost("bookmarks"))
        assertEquals(Screen.Quizzes.route, notificationRouteForHost("quizzes"))
        assertEquals(Screen.Dashboard.route, notificationRouteForHost("dashboard"))
        assertEquals(Screen.History.route, notificationRouteForHost("BOOKMARKS"))
        assertNull(notificationRouteForHost("unknown"))
    }
}
