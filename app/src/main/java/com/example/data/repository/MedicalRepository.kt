package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.*
import com.example.data.model.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

class MedicalRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val userProfileDao = db.userProfileDao()
    private val customMCQDao = db.customMCQDao()
    private val quizScoreDao = db.quizScoreDao()
    private val noteDao = db.noteDao()
    private val bookmarkedMCQDao = db.bookmarkedMCQDao()

    // Moshi & OkHttpClient Init
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- REPLAY/MERGE FLOWS ---
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val quizScores: Flow<List<QuizScore>> = quizScoreDao.getAllScoresFlow()
    val customMCQs: Flow<List<CustomMCQ>> = customMCQDao.getAllCustomMCQsFlow()
    val savedNotes: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val bookmarks: Flow<List<BookmarkedMCQ>> = bookmarkedMCQDao.getBookmarksFlow()

    // Combine predefined and custom questions
    fun observeAllMCQs(): Flow<List<MedicalMCQ>> {
        return customMCQs.combine(bookmarks) { customs, bmarks ->
            val bmarkIds = bmarks.map { it.mcqId }.toSet()
            val predefinedMapped = MedicalDataCatalog.predefinedMCQs

            val customMapped = customs.map { c ->
                MedicalMCQ(
                    id = "cus_${c.id}",
                    question = c.question,
                    optionA = c.optionA,
                    optionB = c.optionB,
                    optionC = c.optionC,
                    optionD = c.optionD,
                    correctAnswer = c.correctAnswer,
                    explanation = c.explanation,
                    referenceTopic = c.referenceTopic,
                    difficultyLevel = c.difficultyLevel,
                    bookSource = c.bookSource,
                    chapterName = c.chapterName,
                    isCustom = true
                )
            }
            predefinedMapped + customMapped
        }
    }

    // --- DB MUTATIONS ---
    suspend fun ensureProfileExists() = withContext(Dispatchers.IO) {
        val existing = userProfileDao.getUserProfile()
        if (existing == null) {
            userProfileDao.insertProfile(UserProfile())
        }
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun addCustomMCQ(mcq: CustomMCQ) = withContext(Dispatchers.IO) {
        customMCQDao.insertCustomMCQ(mcq)
    }

    suspend fun deleteCustomMCQ(id: Long) = withContext(Dispatchers.IO) {
        customMCQDao.deleteCustomMCQ(id)
    }

    suspend fun saveQuizScore(score: QuizScore) = withContext(Dispatchers.IO) {
        quizScoreDao.insertScore(score)
        
        // Boost streak / update dailyProgress of the user profile
        val profile = userProfileDao.getUserProfile() ?: UserProfile()
        val nextProgress = (profile.dailyProgress + 0.15f).coerceAtMost(1.0f)
        val now = System.currentTimeMillis()
        val streak = if (now - profile.lastActiveTime > 20 * 3600 * 1000 && now - profile.lastActiveTime < 48 * 3600 * 1000) {
            profile.studyStreak + 1
        } else if (now - profile.lastActiveTime >= 48 * 3600 * 1000) {
            1 // Reset streak
        } else {
            profile.studyStreak // Kept same if active within the same day
        }
        
        val updatedProfile = profile.copy(
            studyStreak = streak,
            dailyProgress = nextProgress,
            completedChapters = profile.completedChapters + 1,
            lastActiveTime = now
        )
        userProfileDao.insertProfile(updatedProfile)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        quizScoreDao.clearHistory()
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun toggleBookmark(mcqId: String) = withContext(Dispatchers.IO) {
        val all = bookmarkedMCQDao.getBookmarks()
        val exists = all.any { it.mcqId == mcqId }
        if (exists) {
            bookmarkedMCQDao.removeBookmark(mcqId)
        } else {
            bookmarkedMCQDao.addBookmark(BookmarkedMCQ(mcqId))
        }
    }

    // --- GEMINI API CALLER ---
    suspend fun askGemini(prompt: String, systemPrompt: String = "You are Arfi, an expert clinical anatomy and physiology professor."): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing or placeholder. Please provide a real Gemini API Key in the AI Studio Secrets panel."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val fullPrompt = "$systemPrompt\n\nStudent Query: $prompt"
        val requestObj = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = fullPrompt))
                )
            )
        )

        val adapter = moshi.adapter(GeminiRequest::class.java)
        val responseAdapter = moshi.adapter(GeminiResponse::class.java)
        val requestJson = adapter.toJson(requestObj)

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("GeminiAPI", "Failed call: $errorBody")
                    return@withContext "Error: Gemini API returned code ${response.code}. Ensure your API key is correctly configured."
                }
                val bodyText = response.body?.string() ?: return@withContext "Error: Empty response body"
                val geminiRes = responseAdapter.fromJson(bodyText)
                geminiRes?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "Medz Bot could not generate any response parts."
            }
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Exception: ", e)
            "Error: ${e.message ?: "Failed to reach servers. Please check your internet connection."}"
        }
    }

    // --- AI SMART SEARCH MATCHING ---
    suspend fun performSmartSearch(query: String, allMcqs: List<MedicalMCQ>): SearchResults = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) {
            return@withContext SearchResults(emptyList(), emptyList(), "Please enter a valid term above.")
        }

        val q = query.lowercase().trim()

        // 1. Filter local MCQs by question, options, reference topic, or chapter
        val matchedMCQs = allMcqs.filter { mcq ->
            mcq.question.lowercase().contains(q) ||
            mcq.optionA.lowercase().contains(q) ||
            mcq.optionB.lowercase().contains(q) ||
            mcq.optionC.lowercase().contains(q) ||
            mcq.optionD.lowercase().contains(q) ||
            mcq.referenceTopic.lowercase().contains(q) ||
            mcq.chapterName.lowercase().contains(q)
        }

        // 2. Filter local short questions
        val matchedShortQs = MedicalDataCatalog.predefinedShortQuestions.filter { sq ->
            sq.question.lowercase().contains(q) ||
            sq.baseAnswer.lowercase().contains(q) ||
            sq.referenceTopic.lowercase().contains(q) ||
            sq.chapterName.lowercase().contains(q)
        }

        // 3. Optional online AI assistance response for "Explain this concept"
        var aiExplanation = ""
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            val systemPrompt = "You are a senior MBBS anatomical lecturer. Provide a concise, highly clinical definition of this query, explaining the anatomy or physiology behind it, and connecting it to clinical pathologies in 3 bullets."
            aiExplanation = askGemini(query, systemPrompt)
        } else {
            aiExplanation = "To enable professional AI-powered semantic summaries, configure your Gemini API Key in the Secrets panel."
        }

        SearchResults(matchedMCQs, matchedShortQs, aiExplanation)
    }
}

data class SearchResults(
    val mcqs: List<MedicalMCQ>,
    val shortQuestions: List<ShortQuestion>,
    val aiExplanation: String
)
