package com.aistudymentor.data.repository

import androidx.room.withTransaction
import com.aistudymentor.data.local.CachedQuestionEntity
import com.aistudymentor.data.local.toBookmarkEntity
import com.aistudymentor.data.local.CachedQuizResultEntity
import com.aistudymentor.data.local.OfflineDao
import com.aistudymentor.data.local.OfflineSyncStateEntity
import com.aistudymentor.data.local.StudyMentorDatabase

internal class OfflineHistoryReconciler(
    private val database: StudyMentorDatabase,
    private val dao: OfflineDao
) {
    suspend fun reconcile(
        accountId: String,
        previousCursor: String?,
        cursor: String,
        questions: List<CachedQuestionEntity>,
        deletedIds: List<String>,
        quizResults: List<CachedQuizResultEntity>
    ) {
        database.withTransaction {
            if (previousCursor == null) {
                dao.clearQuestions(accountId)
                dao.clearBookmarks(accountId)
            }
            dao.insertQuestions(questions)
            questions.forEach { question ->
                if (question.isBookmarked) {
                    val viewedAt = dao.bookmarkLastViewedAt(accountId, question.questionId)
                    dao.insertBookmarks(listOf(question.toBookmarkEntity().copy(lastViewedAt = viewedAt)))
                } else {
                    dao.deleteBookmark(accountId, question.questionId)
                }
            }
            if (deletedIds.isNotEmpty()) {
                dao.deleteQuestions(accountId, deletedIds)
                dao.deleteBookmarks(accountId, deletedIds)
            }
            dao.saveSyncState(OfflineSyncStateEntity(accountId, cursor))
            dao.clearQuizResults(accountId)
            dao.insertQuizResults(quizResults)
        }
    }
}
