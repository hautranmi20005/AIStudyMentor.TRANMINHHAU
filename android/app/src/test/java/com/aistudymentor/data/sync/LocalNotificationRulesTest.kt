package com.aistudymentor.data.sync

import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant

class LocalNotificationRulesTest {
    private val now = LocalDateTime.parse("2026-05-01T12:00:00")

    @Test
    fun bookmarkReminderStartsAtThreeDaysAndResetsAfterOpeningDetail() {
        val atThreshold = bookmark(lastViewedAt = now.minusDays(3).toString())
        val recentlyViewed = bookmark(lastViewedAt = now.minusDays(3).plusSeconds(1).toString())

        assertTrue(LocalNotificationRules.shouldRemindAboutBookmarks(listOf(atThreshold), now))
        assertFalse(LocalNotificationRules.shouldRemindAboutBookmarks(listOf(recentlyViewed), now))
    }

    @Test
    fun repeatedMistakeRequiresTheSameTopicAcrossTwoRecentAttempts() {
        val results = listOf(
            result("attempt-1", """[{"topic":"Linear equations","isCorrect":false}]"""),
            result("attempt-2", """[{"topic":"Linear equations","isCorrect":false}]"""),
            result("attempt-3", """[{"topic":"Geometry","isCorrect":false}]""")
        )

        assertTrue(LocalNotificationRules.hasRepeatedMistakes(results, now))
        assertFalse(LocalNotificationRules.hasRepeatedMistakes(results.take(1), now))
    }

    @Test
    fun localNotificationPeriodUsesPreferenceTimezone() {
        val boundary = Instant.parse("2026-01-05T00:30:00Z")
        val utc = LocalNotificationRules.periodKey(boundary.atZone(ZoneId.of("UTC")))
        val losAngeles = LocalNotificationRules.periodKey(boundary.atZone(ZoneId.of("America/Los_Angeles")))

        assertTrue(utc.endsWith("W02"))
        assertTrue(losAngeles.endsWith("W01"))
    }

    private fun bookmark(lastViewedAt: String) = CachedBookmarkEntity(
        accountId = "account",
        questionId = "question",
        subject = "Math",
        questionSnippet = "Solve",
        hasImage = false,
        status = "ANSWERED",
        createdAt = now.minusDays(10).toString(),
        lastViewedAt = lastViewedAt
    )

    private fun result(attemptId: String, details: String) = CachedQuizResultEntity(
        accountId = "account",
        attemptId = attemptId,
        quizId = "quiz-$attemptId",
        subject = "Math",
        score = 0,
        totalQuestions = 1,
        correctCount = 0,
        submittedAt = now.minusDays(2).toString(),
        detailsJson = details
    )
}
