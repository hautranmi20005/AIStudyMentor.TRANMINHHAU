package com.aistudymentor.data.local

import androidx.room.Entity

@Entity(tableName = "cached_questions", primaryKeys = ["accountId", "questionId"])
data class CachedQuestionEntity(
    val accountId: String,
    val questionId: String,
    val subject: String,
    val questionSnippet: String,
    val hasImage: Boolean,
    val imageUrl: String? = null,
    val isBookmarked: Boolean,
    val status: String,
    val createdAt: String?,
    val questionText: String = questionSnippet,
    val answerJson: String? = null
) {
    constructor(
        accountId: String,
        questionId: String,
        subject: String,
        questionSnippet: String,
        hasImage: Boolean,
        isBookmarked: Boolean,
        status: String,
        createdAt: String?
    ) : this(
        accountId, questionId, subject, questionSnippet, hasImage, null,
        isBookmarked, status, createdAt, questionSnippet, null
    )
}

@Entity(tableName = "cached_bookmarks", primaryKeys = ["accountId", "questionId"])
data class CachedBookmarkEntity(
    val accountId: String,
    val questionId: String,
    val subject: String,
    val questionSnippet: String,
    val hasImage: Boolean,
    val imageUrl: String? = null,
    val status: String,
    val createdAt: String?,
    val lastViewedAt: String? = null
) {
    constructor(
        accountId: String,
        questionId: String,
        subject: String,
        questionSnippet: String,
        hasImage: Boolean,
        status: String,
        createdAt: String?
    ) : this(accountId, questionId, subject, questionSnippet, hasImage, null, status, createdAt, null)
}

@Entity(tableName = "cached_quiz_results", primaryKeys = ["accountId", "attemptId"])
data class CachedQuizResultEntity(
    val accountId: String,
    val attemptId: String,
    val quizId: String,
    val subject: String,
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val submittedAt: String?,
    val detailsJson: String
)

@Entity(tableName = "pending_bookmark_changes", primaryKeys = ["accountId", "questionId"])
data class PendingBookmarkChangeEntity(
    val accountId: String,
    val questionId: String,
    val isBookmarked: Boolean
)

@Entity(tableName = "offline_sync_state")
data class OfflineSyncStateEntity(
    @androidx.room.PrimaryKey val accountId: String,
    val cursor: String
)

@Entity(tableName = "cached_dashboard_metrics", primaryKeys = ["accountId", "range", "timezone"])
data class CachedDashboardMetricsEntity(
    val accountId: String,
    val range: String,
    val timezone: String,
    val metricsJson: String,
    val cachedAt: String
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @androidx.room.PrimaryKey val accountId: String,
    val bookmarkReviewEnabled: Boolean = true,
    val repeatedMistakeEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val timezone: String = "UTC",
    val pendingUpload: Boolean = false
)

@Entity(tableName = "local_notification_deliveries", primaryKeys = ["accountId", "type", "periodKey"])
data class LocalNotificationDeliveryEntity(
    val accountId: String,
    val type: String,
    val periodKey: String,
    val createdAt: String
)

fun CachedQuestionEntity.toBookmarkEntity(): CachedBookmarkEntity = CachedBookmarkEntity(
    accountId = accountId,
    questionId = questionId,
    subject = subject,
    questionSnippet = questionSnippet,
    hasImage = hasImage,
    imageUrl = imageUrl,
    status = status,
    createdAt = createdAt
)
