package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserProfile::class,
        CustomMCQ::class,
        CustomSEQ::class,
        QuizScore::class,
        Note::class,
        BookmarkedMCQ::class,
        VideoLecture::class,
        AppPreferences::class,
        CustomUploadedFile::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun customMCQDao(): CustomMCQDao
    abstract fun customSEQDao(): CustomSEQDao
    abstract fun customUploadedFileDao(): CustomUploadedFileDao
    abstract fun quizScoreDao(): QuizScoreDao
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkedMCQDao(): BookmarkedMCQDao
    abstract fun videoLectureDao(): VideoLectureDao
    abstract fun appPreferencesDao(): AppPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medz_with_arfi_db"
                )
                .fallbackToDestructiveMigration() // Graceful upgrade protocol
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
