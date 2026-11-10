package com.aistudymentor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {
    @Query("SELECT * FROM cached_questions WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeQuestions(accountId: String): Flow<List<CachedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CachedQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CachedQuestionEntity)

    @Query("DELETE FROM cached_questions WHERE accountId = :accountId")
    suspend fun clearQuestions(accountId: String)

    @Query("SELECT * FROM cached_bookmarks WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeBookmarks(accountId: String): Flow<List<CachedBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<CachedBookmarkEntity>)

    @Query("DELETE FROM cached_bookmarks WHERE accountId = :accountId")
    suspend fun clearBookmarks(accountId: String)

    @Query("SELECT * FROM cached_quiz_results WHERE accountId = :accountId ORDER BY submittedAt DESC")
    fun observeQuizResults(accountId: String): Flow<List<CachedQuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResults(results: List<CachedQuizResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: CachedQuizResultEntity)

    @Query("DELETE FROM cached_quiz_results WHERE accountId = :accountId")
    suspend fun clearQuizResults(accountId: String)
}
