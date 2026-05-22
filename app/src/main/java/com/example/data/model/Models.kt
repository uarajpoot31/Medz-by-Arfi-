package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "primary_user",
    val name: String = "Dr. Student",
    val email: String = "student@medzwitharfi.edu",
    val studyStreak: Int = 3,
    val dailyProgress: Float = 0.65f,
    val weakTopics: String = "Embryology, Endocrine System, Pelvis",
    val completedChapters: Int = 12,
    val lastActiveTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_mcqs")
data class CustomMCQ(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val explanation: String,
    val referenceTopic: String,
    val difficultyLevel: String, // "Easy", "Medium", "Hard"
    val isCustom: Boolean = true,
    val bookSource: String, // "Snell's Clinical Anatomy" or "Ross and Wilson Anatomy & Physiology"
    val chapterName: String
)

@Entity(tableName = "quiz_scores")
data class QuizScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val percentage: Float,
    val timeTakenSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val quizType: String, // "Chapter-wise", "Topic-wise", "Daily", "Mock"
    val chapterOrBook: String
)

@Entity(tableName = "saved_notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val chapter: String,
    val bookSource: String, // "Snell", "Ross"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarked_mcqs")
data class BookmarkedMCQ(
    @PrimaryKey val mcqId: String, // Can represent "pre_X" for predefined, or "cus_X" for custom
    val timestamp: Long = System.currentTimeMillis()
)

// Predefined medical Quiz representations (In-Memory default database merged with Custom ones)
data class MedicalMCQ(
    val id: String, // "pre_1", "pre_2"... or "cus_X"
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val explanation: String,
    val referenceTopic: String,
    val difficultyLevel: String, // "Easy", "Medium", "Hard"
    val bookSource: String,
    val chapterName: String,
    val isCustom: Boolean = false
)

data class ShortQuestion(
    val id: String,
    val question: String,
    val baseAnswer: String,
    val bookSource: String,
    val chapterName: String,
    val referenceTopic: String
)
