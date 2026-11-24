package com.aistudymentor.data.sync

import com.aistudymentor.data.local.CachedBookmarkEntity
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.google.gson.JsonParser
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.IsoFields

internal object LocalNotificationRules {
    const val BOOKMARK_REVIEW = "BOOKMARK_REVIEW"
    const val REPEATED_MISTAKE = "REPEATED_MISTAKE"
    const val WEEKLY_SUMMARY = "WEEKLY_SUMMARY"

    fun periodKey(localNow: ZonedDateTime): String {
        return "${localNow.get(IsoFields.WEEK_BASED_YEAR)}-W%02d".format(
            localNow.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )
    }

    fun shouldRemindAboutBookmarks(
        bookmarks: List<CachedBookmarkEntity>,
        now: LocalDateTime
    ): Boolean {
        val threshold = now.minusDays(3)
        return bookmarks.any { bookmark ->
            parseDate(bookmark.lastViewedAt ?: bookmark.createdAt)?.let { !it.isAfter(threshold) } == true
        }
    }

    fun hasRepeatedMistakes(results: List<CachedQuizResultEntity>, now: LocalDateTime): Boolean {
        val cutoff = now.minusDays(30)
        val attemptsByTopic = mutableMapOf<Pair<String, String>, MutableSet<String>>()
        results.forEach attemptLoop@{ attempt ->
            val submittedAt = parseDate(attempt.submittedAt) ?: return@attemptLoop
            if (submittedAt.isBefore(cutoff)) return@attemptLoop
            val details = runCatching { JsonParser.parseString(attempt.detailsJson).asJsonArray }.getOrNull()
                ?: return@attemptLoop
            details.forEach resultLoop@{ item ->
                val result = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@resultLoop
                val incorrect = result.get("isCorrect")?.takeIf { !it.isJsonNull }?.asBoolean == false
                val topic = result.get("topic")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
                if (incorrect && topic.isNotEmpty()) {
                    attemptsByTopic.getOrPut(attempt.subject to topic) { mutableSetOf() }.add(attempt.attemptId)
                }
            }
        }
        return attemptsByTopic.values.any { it.size >= 2 }
    }

    fun hasRecentActivity(
        questions: List<CachedQuestionEntity>,
        results: List<CachedQuizResultEntity>,
        now: LocalDateTime
    ): Boolean {
        val cutoff = now.minusDays(7)
        return questions.any { parseDate(it.createdAt)?.isBefore(cutoff) == false } ||
            results.any { parseDate(it.submittedAt)?.isBefore(cutoff) == false }
    }

    fun isQuietHours(now: LocalDateTime, start: String?, end: String?): Boolean {
        val quietStart = start?.let { runCatching { java.time.LocalTime.parse(it) }.getOrNull() } ?: return false
        val quietEnd = end?.let { runCatching { java.time.LocalTime.parse(it) }.getOrNull() } ?: return false
        if (quietStart == quietEnd) return false
        val current = now.toLocalTime()
        return if (quietStart.isBefore(quietEnd)) {
            !current.isBefore(quietStart) && current.isBefore(quietEnd)
        } else {
            !current.isBefore(quietStart) || current.isBefore(quietEnd)
        }
    }

    private fun parseDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toLocalDateTime() }.getOrNull()
    }
}
