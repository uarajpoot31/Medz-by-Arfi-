package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)
}

@Dao
interface CustomMCQDao {
    @Query("SELECT * FROM custom_mcqs ORDER BY id DESC")
    fun getAllCustomMCQsFlow(): Flow<List<CustomMCQ>>

    @Query("SELECT * FROM custom_mcqs")
    suspend fun getAllCustomMCQs(): List<CustomMCQ>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMCQ(mcq: CustomMCQ)

    @Query("DELETE FROM custom_mcqs WHERE id = :id")
    suspend fun deleteCustomMCQ(id: Long)
}

@Dao
interface QuizScoreDao {
    @Query("SELECT * FROM quiz_scores ORDER BY timestamp DESC")
    fun getAllScoresFlow(): Flow<List<QuizScore>>

    @Query("SELECT * FROM quiz_scores")
    suspend fun getAllScores(): List<QuizScore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: QuizScore)

    @Query("DELETE FROM quiz_scores")
    suspend fun clearHistory()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM saved_notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM saved_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM saved_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface BookmarkedMCQDao {
    @Query("SELECT * FROM bookmarked_mcqs ORDER BY timestamp DESC")
    fun getBookmarksFlow(): Flow<List<BookmarkedMCQ>>

    @Query("SELECT * FROM bookmarked_mcqs")
    suspend fun getBookmarks(): List<BookmarkedMCQ>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: BookmarkedMCQ)

    @Query("DELETE FROM bookmarked_mcqs WHERE mcqId = :id")
    suspend fun removeBookmark(id: String)
}
