package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "primary_user",
    val name: String = "Dr. Student",
    val email: String = "student@medzwitharfi.edu",
    val collegeName: String = "Punjab Medical College",
    val mobileNumber: String = "+92-300-1234567",
    val studyStreak: Int = 3,
    val dailyProgress: Float = 0.65f,
    val weakTopics: String = "Embryology, Endocrine System, Pelvis",
    val completedChapters: Int = 12,
    val lastActiveTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_lectures")
data class VideoLecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val duration: String,
    val description: String,
    val bookSource: String, // "Snell's Clinical Anatomy" or "Ross and Wilson Anatomy & Physiology"
    val chapterName: String,
    val videoUrl: String, // dynamic mock or real url
    val isCustom: Boolean = false
) : Serializable

@Entity(tableName = "app_preferences")
data class AppPreferences(
    @PrimaryKey val id: String = "singleton_pref",
    val appName: String = "Medz with Arfi",
    val logoIconName: String = "MedicalServices", // MedicalServices, LocalHospital, Favorite, Medication, Healing
    val logoBgColorHex: String = "#8E1439", // Hex code
    val customLogoUri: String? = null,
    val customGeminiApiKey: String? = null
) : Serializable

@Entity(tableName = "custom_uploaded_files")
data class CustomUploadedFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileUri: String, // local filesystem path / persistent identifier
    val fileSize: String,
    val fileType: String, // "image", "pdf", "docx", "other"
    val uploadedAt: Long = System.currentTimeMillis()
) : Serializable

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

@Entity(tableName = "custom_seqs")
data class CustomSEQ(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val baseAnswer: String,
    val bookSource: String, // "Snell's Clinical Anatomy" or "Ross and Wilson Anatomy & Physiology"
    val chapterName: String,
    val referenceTopic: String,
    val isCustom: Boolean = true
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

@Entity(tableName = "feedback_notifications")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class FeedbackNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentName: String,
    val studentEmail: String,
    val itemType: String, // "Lecture" or "Material/Note"
    val itemId: String,
    val itemTitle: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

