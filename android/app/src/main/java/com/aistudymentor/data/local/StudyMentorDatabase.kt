package com.aistudymentor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedQuestionEntity::class, CachedBookmarkEntity::class, CachedQuizResultEntity::class],
    version = 1,
    exportSchema = true
)
abstract class StudyMentorDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        @Volatile
        private var instance: StudyMentorDatabase? = null

        fun getInstance(context: Context): StudyMentorDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StudyMentorDatabase::class.java,
                "study-mentor-offline.db"
            ).build().also { instance = it }
        }
    }
}
