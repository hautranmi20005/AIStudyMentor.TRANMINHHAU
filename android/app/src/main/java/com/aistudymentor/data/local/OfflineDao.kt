package com.aistudymentor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {
    @Query("SELECT * FROM cached_questions WHERE accountId = :accountId AND questionId = :questionId LIMIT 1")
    suspend fun getQuestion(accountId: String, questionId: String): CachedQuestionEntity?

    @Query("SELECT * FROM cached_questions WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeQuestions(accountId: String): Flow<List<CachedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CachedQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CachedQuestionEntity)

    @Query("DELETE FROM cached_questions WHERE accountId = :accountId")
    suspend fun clearQuestions(accountId: String)

    @Query("DELETE FROM cached_questions WHERE accountId = :accountId AND questionId IN (:questionIds)")
    suspend fun deleteQuestions(accountId: String, questionIds: List<String>)

    @Query("SELECT * FROM cached_bookmarks WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeBookmarks(accountId: String): Flow<List<CachedBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<CachedBookmarkEntity>)

    @Query("DELETE FROM cached_bookmarks WHERE accountId = :accountId")
    suspend fun clearBookmarks(accountId: String)

    @Query("DELETE FROM cached_bookmarks WHERE accountId = :accountId AND questionId IN (:questionIds)")
    suspend fun deleteBookmarks(accountId: String, questionIds: List<String>)

    @Query("DELETE FROM cached_bookmarks WHERE accountId = :accountId AND questionId = :questionId")
    suspend fun deleteBookmark(accountId: String, questionId: String)

    @Query("SELECT lastViewedAt FROM cached_bookmarks WHERE accountId = :accountId AND questionId = :questionId")
    suspend fun bookmarkLastViewedAt(accountId: String, questionId: String): String?

    @Query("SELECT COUNT(*) FROM pending_bookmark_changes WHERE accountId = :accountId")
    fun observePendingBookmarkCount(accountId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_bookmark_changes WHERE accountId = :accountId")
    suspend fun pendingBookmarkCount(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePendingBookmarkChange(change: PendingBookmarkChangeEntity)

    @Query("DELETE FROM pending_bookmark_changes WHERE accountId = :accountId AND questionId = :questionId")
    suspend fun deletePendingBookmarkChange(accountId: String, questionId: String)

    @Query("SELECT * FROM pending_bookmark_changes WHERE accountId = :accountId")
    suspend fun pendingBookmarkChanges(accountId: String): List<PendingBookmarkChangeEntity>

    @Query("SELECT * FROM cached_quiz_results WHERE accountId = :accountId ORDER BY submittedAt DESC")
    fun observeQuizResults(accountId: String): Flow<List<CachedQuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResults(results: List<CachedQuizResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: CachedQuizResultEntity)

    @Query("DELETE FROM cached_quiz_results WHERE accountId = :accountId")
    suspend fun clearQuizResults(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncState(state: OfflineSyncStateEntity)

    @Query("SELECT cursor FROM offline_sync_state WHERE accountId = :accountId")
    suspend fun syncCursor(accountId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotificationPreferences(preferences: NotificationPreferencesEntity)

    @Query("SELECT * FROM notification_preferences WHERE accountId = :accountId LIMIT 1")
    suspend fun getNotificationPreferences(accountId: String): NotificationPreferencesEntity?

    @Query("SELECT * FROM notification_preferences WHERE accountId = :accountId LIMIT 1")
    fun observeNotificationPreferences(accountId: String): Flow<NotificationPreferencesEntity?>

    @Query("UPDATE notification_preferences SET pendingUpload = 0 WHERE accountId = :accountId")
    suspend fun markNotificationPreferencesSynced(accountId: String)

    @Query("SELECT * FROM cached_bookmarks WHERE accountId = :accountId")
    suspend fun notificationBookmarks(accountId: String): List<CachedBookmarkEntity>

    @Query("SELECT * FROM cached_questions WHERE accountId = :accountId")
    suspend fun notificationQuestions(accountId: String): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_quiz_results WHERE accountId = :accountId")
    suspend fun notificationQuizResults(accountId: String): List<CachedQuizResultEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocalNotificationDelivery(delivery: LocalNotificationDeliveryEntity)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM local_notification_deliveries " +
            "WHERE accountId = :accountId AND type = :type AND periodKey = :periodKey)"
    )
    suspend fun hasLocalNotificationBeenSent(accountId: String, type: String, periodKey: String): Boolean

    @Query(
        "INSERT OR IGNORE INTO local_notification_deliveries(accountId, type, periodKey, createdAt) " +
            "VALUES(:accountId, :type, :periodKey, :createdAt)"
    )
    suspend fun recordLocalNotification(accountId: String, type: String, periodKey: String, createdAt: String)
}
