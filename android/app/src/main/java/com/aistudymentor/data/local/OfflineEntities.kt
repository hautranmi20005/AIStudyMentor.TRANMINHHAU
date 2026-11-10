package com.aistudymentor.data.local

import androidx.room.Entity

@Entity(tableName = "cached_questions", primaryKeys = ["accountId", "questionId"])
data class CachedQuestionEntity(
    val accountId: String,
    val questionId: String,
    val subject: String,
    val questionSnippet: String,
    val hasImage: Boolean,
    val isBookmarked: Boolean,
    val status: String,
    val createdAt: String?
)

@Entity(tableName = "cached_bookmarks", primaryKeys = ["accountId", "questionId"])
data class CachedBookmarkEntity(
    val accountId: String,
    val questionId: String,
    val subject: String,
    val questionSnippet: String,
    val hasImage: Boolean,
    val status: String,
    val createdAt: String?
)

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
