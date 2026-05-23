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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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

@JsonClass(generateAdapter = true)
data class CloudSyncData(
    val files: List<CustomUploadedFile> = emptyList(),
    val mcqs: List<CustomMCQ> = emptyList(),
    val seqs: List<CustomSEQ> = emptyList(),
    val customGeminiApiKey: String = "",
    val lectures: List<VideoLecture> = emptyList(),
    val profiles: List<UserProfile> = emptyList(),
    val notifications: List<FeedbackNotification> = emptyList()
)

class MedicalRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val userProfileDao = db.userProfileDao()
    private val customMCQDao = db.customMCQDao()
    private val customSEQDao = db.customSEQDao()
    private val customUploadedFileDao = db.customUploadedFileDao()
    private val quizScoreDao = db.quizScoreDao()
    private val noteDao = db.noteDao()
    private val bookmarkedMCQDao = db.bookmarkedMCQDao()
    private val videoLectureDao = db.videoLectureDao()
    private val appPreferencesDao = db.appPreferencesDao()
    private val feedbackNotificationDao = db.feedbackNotificationDao()


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
    val allProfiles: Flow<List<UserProfile>> = userProfileDao.getAllProfilesFlow()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val quizScores: Flow<List<QuizScore>> = quizScoreDao.getAllScoresFlow()
    val customMCQs: Flow<List<CustomMCQ>> = customMCQDao.getAllCustomMCQsFlow()
    val customSEQs: Flow<List<CustomSEQ>> = customSEQDao.getAllCustomSEQsFlow()
    val customUploadedFiles: Flow<List<CustomUploadedFile>> = customUploadedFileDao.getAllUploadedFilesFlow()
    val savedNotes: Flow<List<Note>> = noteDao.getAllNotesFlow()
    val bookmarks: Flow<List<BookmarkedMCQ>> = bookmarkedMCQDao.getBookmarksFlow()
    val allVideoLectures: Flow<List<VideoLecture>> = videoLectureDao.getAllVideoLecturesFlow()
    val appPreferences: Flow<AppPreferences?> = appPreferencesDao.getAppPreferencesFlow()
    val allNotifications: Flow<List<FeedbackNotification>> = feedbackNotificationDao.getAllNotificationsFlow()

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

    // Combine predefined and custom short questions / SEQs
    fun observeAllSEQs(): Flow<List<ShortQuestion>> {
        return customSEQs.map { customs ->
            val predefinedMapped = MedicalDataCatalog.predefinedShortQuestions
            val customMapped = customs.map { c ->
                ShortQuestion(
                    id = "cus_${c.id}",
                    question = c.question,
                    baseAnswer = c.baseAnswer,
                    bookSource = c.bookSource,
                    chapterName = c.chapterName,
                    referenceTopic = c.referenceTopic
                )
            }
            predefinedMapped + customMapped
        }
    }

    // --- DB MUTATIONS ---
    suspend fun ensureProfileExists() = withContext(Dispatchers.IO) {
        // Core profile
        val existing = userProfileDao.getUserProfile()
        if (existing == null) {
            userProfileDao.insertProfile(UserProfile())
        }

        // Seeding App settings
        val existingPref = appPreferencesDao.getAppPreferences()
        if (existingPref == null) {
            appPreferencesDao.insertAppPreferences(AppPreferences())
        }

        // Seeding some starter video lectures
        val existingLectures = videoLectureDao.getAllVideoLecturesFlow().firstOrNull() ?: emptyList()
        if (existingLectures.isEmpty()) {
            val lectures = listOf(
                VideoLecture(
                    title = "Anatomy: Upper Limb Brachial Plexus",
                    duration = "14:22 mins",
                    description = "Full clinical walkthrough of the roots, trunks, divisions, cords, and branches of the brachial plexus. Excellent for clinical diagnostics.",
                    bookSource = MedicalDataCatalog.BookSnellAnatomy,
                    chapterName = "Upper Limb",
                    videoUrl = "https://www.youtube.com/embed/g-Vb6L02G90"
                ),
                VideoLecture(
                    title = "Physiology: Action Potential of the Pacemaker Myocardium",
                    duration = "10:45 mins",
                    description = "Comprehensive ionic overview of cardiac action potentials of Sinoatrial nodes. Explaining hyperpolarization and calcium currents.",
                    bookSource = MedicalDataCatalog.BookRossPhysiology,
                    chapterName = "Cardiovascular System",
                    videoUrl = "https://www.youtube.com/embed/v7Q9v7_762"
                ),
                VideoLecture(
                    title = "Anatomy: Cranial Nerves Clinical Pathways & Foramina",
                    duration = "18:15 mins",
                    description = "An interactive review of core cranial nerve roots, anatomical exit skull foramina, and relevant clinical pathologies (Snell Chapter 12).",
                    bookSource = MedicalDataCatalog.BookSnellAnatomy,
                    chapterName = "Clinical Anatomy",
                    videoUrl = "https://www.youtube.com/embed/Z0Xn3V_C_F0"
                ),
                VideoLecture(
                    title = "Physiology: Nephron Countercurrent Multiplier Mechanics",
                    duration = "15:30 mins",
                    description = "Detailed ionic walkthrough of nephron clearance, loop of Henle concentration, and proximal convoluted tubule absorption.",
                    bookSource = MedicalDataCatalog.BookRossPhysiology,
                    chapterName = "Urinary System",
                    videoUrl = "https://www.youtube.com/embed/v7Q9v7_763"
                )
            )
            lectures.forEach { videoLectureDao.insertVideoLecture(it) }
        }
    }

    suspend fun getProfileByEmail(email: String): UserProfile? = withContext(Dispatchers.IO) {
        userProfileDao.getProfileByEmail(email)
    }

    suspend fun getAllProfiles(): List<UserProfile> = withContext(Dispatchers.IO) {
        userProfileDao.getAllProfiles()
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
        pushAllCustomData()
    }

    suspend fun addCustomMCQ(mcq: CustomMCQ) = withContext(Dispatchers.IO) {
        customMCQDao.insertCustomMCQ(mcq)
        pushAllCustomData()
    }

    suspend fun deleteCustomMCQ(id: Long) = withContext(Dispatchers.IO) {
        customMCQDao.deleteCustomMCQ(id)
        pushAllCustomData()
    }

    suspend fun deleteCustomMCQByIdString(mcqIdString: String) = withContext(Dispatchers.IO) {
        if (mcqIdString.startsWith("cus_")) {
            val numericId = mcqIdString.substringAfter("cus_").toLongOrNull() ?: 0L
            customMCQDao.deleteCustomMCQ(numericId)
            pushAllCustomData()
        }
    }

    suspend fun addCustomSEQ(seq: CustomSEQ) = withContext(Dispatchers.IO) {
        customSEQDao.insertCustomSEQ(seq)
        pushAllCustomData()
    }

    suspend fun deleteCustomSEQ(id: Long) = withContext(Dispatchers.IO) {
        customSEQDao.deleteCustomSEQ(id)
        pushAllCustomData()
    }

    suspend fun deleteCustomSEQByIdString(seqIdString: String) = withContext(Dispatchers.IO) {
        if (seqIdString.startsWith("cus_")) {
            val numericId = seqIdString.substringAfter("cus_").toLongOrNull() ?: 0L
            customSEQDao.deleteCustomSEQ(numericId)
            pushAllCustomData()
        }
    }

    suspend fun updateAppPreferences(prefs: AppPreferences) = withContext(Dispatchers.IO) {
        appPreferencesDao.insertAppPreferences(prefs)
        pushAllCustomData()
    }

    suspend fun addCustomUploadedFile(file: CustomUploadedFile) = withContext(Dispatchers.IO) {
        customUploadedFileDao.insertUploadedFile(file)
        pushAllCustomData()
    }

    suspend fun deleteCustomUploadedFile(id: Long) = withContext(Dispatchers.IO) {
        customUploadedFileDao.deleteUploadedFile(id)
        pushAllCustomData()
    }

    suspend fun addVideoLecture(lecture: VideoLecture) = withContext(Dispatchers.IO) {
        videoLectureDao.insertVideoLecture(lecture)
        pushAllCustomData()
    }

    suspend fun deleteVideoLecture(id: Long) = withContext(Dispatchers.IO) {
        videoLectureDao.deleteVideoLecture(id)
        pushAllCustomData()
    }

    suspend fun saveQuizScore(score: QuizScore, userEmail: String) = withContext(Dispatchers.IO) {
        quizScoreDao.insertScore(score)
        
        // Boost streak / update dailyProgress of the user profile
        val profile = userProfileDao.getProfileByEmail(userEmail) ?: UserProfile(id = userEmail, email = userEmail)
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

    suspend fun addFeedbackNotification(notification: FeedbackNotification) = withContext(Dispatchers.IO) {
        feedbackNotificationDao.insertNotification(notification)
        pushAllCustomData()
    }

    suspend fun deleteNotificationById(id: Long) = withContext(Dispatchers.IO) {
        feedbackNotificationDao.deleteNotification(id)
        pushAllCustomData()
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        feedbackNotificationDao.clearNotifications()
        pushAllCustomData()
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
        val prefs = appPreferencesDao.getAppPreferences()
        val apiKey = if (!prefs?.customGeminiApiKey.isNullOrBlank()) {
            prefs?.customGeminiApiKey ?: ""
        } else {
            BuildConfig.GEMINI_API_KEY
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing or placeholder. Please provide a real Gemini API Key inside the Admin Settings Panel or AI Studio Secrets panel."
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
    suspend fun performSmartSearch(query: String, allMcqs: List<MedicalMCQ>, allShortQs: List<ShortQuestion>): SearchResults = withContext(Dispatchers.IO) {
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
        val matchedShortQs = allShortQs.filter { sq ->
            sq.question.lowercase().contains(q) ||
            sq.baseAnswer.lowercase().contains(q) ||
            sq.referenceTopic.lowercase().contains(q) ||
            sq.chapterName.lowercase().contains(q)
        }

        // 3. Optional online AI assistance response for "Explain this concept"
        var aiExplanation = ""
        val prefs = appPreferencesDao.getAppPreferences()
        val apiKey = if (!prefs?.customGeminiApiKey.isNullOrBlank()) {
            prefs?.customGeminiApiKey ?: ""
        } else {
            BuildConfig.GEMINI_API_KEY
        }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            val systemPrompt = "You are a senior MBBS anatomical lecturer. Provide a concise, highly clinical definition of this query, explaining the anatomy or physiology behind it, and connecting it to clinical pathologies in 3 bullets."
            aiExplanation = askGemini(query, systemPrompt)
        } else {
            aiExplanation = "To enable professional AI-powered semantic summaries, configure your Gemini API Key in the Admin Settings panel."
        }

        SearchResults(matchedMCQs, matchedShortQs, aiExplanation)
    }

    // --- SHARED CLOUD SYNCHRONIZATION WITH KVDB.IO ---
    private val cloudSyncAdapter by lazy {
        moshi.adapter(CloudSyncData::class.java)
    }

    suspend fun pushAllCustomData() = withContext(Dispatchers.IO) {
        try {
            val filesList = customUploadedFileDao.getAllUploadedFiles()
            val mcqsList = customMCQDao.getAllCustomMCQs()
            val seqsList = customSEQDao.getAllCustomSEQs()
            val appPref = appPreferencesDao.getAppPreferences() ?: AppPreferences()
            val apiKey = appPref.customGeminiApiKey ?: ""
            val lecturesList = videoLectureDao.getAllVideoLectures()
            val profilesList = userProfileDao.getAllProfiles()
            val notificationsList = feedbackNotificationDao.getAllNotifications()

            val syncData = CloudSyncData(
                files = filesList,
                mcqs = mcqsList,
                seqs = seqsList,
                customGeminiApiKey = apiKey,
                lectures = lecturesList,
                profiles = profilesList,
                notifications = notificationsList
            )

            val jsonString = cloudSyncAdapter.toJson(syncData)
            val request = Request.Builder()
                .url("https://kvdb.io/pk_medzwitharfi_v817/alldata")
                .put(jsonString.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d("SyncCloud", "Successfully pushed consolidated data to cloud. Status code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SyncCloud", "Failed to push consolidated cloud data: ", e)
        }
    }

    suspend fun pullAllCustomData() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://kvdb.io/pk_medzwitharfi_v817/alldata")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body != "not found") {
                        val syncData = cloudSyncAdapter.fromJson(body)
                        if (syncData != null) {
                            // 1. Sync Files
                            customUploadedFileDao.clearUploadedFiles()
                            customUploadedFileDao.insertUploadedFiles(syncData.files)

                            // 2. Sync MCQs
                            customMCQDao.clearCustomMCQs()
                            customMCQDao.insertCustomMCQs(syncData.mcqs)

                            // 3. Sync SEQs
                            customSEQDao.clearCustomSEQs()
                            customSEQDao.insertCustomSEQs(syncData.seqs)

                            // 4. Sync lectures
                            if (syncData.lectures.isNotEmpty()) {
                                videoLectureDao.clearVideoLectures()
                                videoLectureDao.insertVideoLectures(syncData.lectures)
                            }

                            // 5. Sync Gemini API Key
                            if (syncData.customGeminiApiKey.isNotEmpty()) {
                                val existingPref = appPreferencesDao.getAppPreferences() ?: AppPreferences()
                                if (existingPref.customGeminiApiKey != syncData.customGeminiApiKey) {
                                    appPreferencesDao.insertAppPreferences(existingPref.copy(customGeminiApiKey = syncData.customGeminiApiKey))
                                }
                            }

                            // 6. Sync User Profiles (Collaborative roster)
                            val localProfiles = userProfileDao.getAllProfiles()
                            val localMap = localProfiles.associateBy { it.email }

                            syncData.profiles.forEach { pulledProfile ->
                                val localProfile = localMap[pulledProfile.email]
                                if (localProfile == null) {
                                    userProfileDao.insertProfile(pulledProfile)
                                } else if (pulledProfile.lastActiveTime > localProfile.lastActiveTime) {
                                    userProfileDao.insertProfile(pulledProfile)
                                }
                            }

                            // 7. Sync Feedback Notifications
                            val localNotifications = feedbackNotificationDao.getAllNotifications()
                            val localSignatures = localNotifications.map { "${it.studentEmail}_${it.itemId}_${it.timestamp}" }.toSet()

                            syncData.notifications.forEach { pulledNotification ->
                                val sig = "${pulledNotification.studentEmail}_${pulledNotification.itemId}_${pulledNotification.timestamp}"
                                if (!localSignatures.contains(sig)) {
                                    feedbackNotificationDao.insertNotification(pulledNotification.copy(id = 0))
                                }
                            }
                        }
                    }
                }
            }
            Log.d("SyncCloud", "Successfully synced and downloaded consolidated cloud data.")
        } catch (e: Exception) {
            Log.e("SyncCloud", "Failed to pull consolidated cloud sync data: ", e)
        }
    }

    suspend fun sendWelcomeEmailViaNetwork(email: String, name: String) = withContext(Dispatchers.IO) {
        try {
            // Send secure, beautifully structured Web3Forms form submission to medzwitharfi@gmail.com
            // AND trigger a highly professional outbound autoresponder to the student's email ($email) of their sign-up.
            val bodyJson = """
                {
                    "access_key": "c82ef9ff-3dc9-42b7-a3f2-1d52d9a5b3a8",
                    "from_name": "Medz with Arfi Support Table",
                    "subject": "🎉 Welcome to Medz with Arfi!",
                    "email": "$email",
                    "name": "$name",
                    "message": "Assalamu Alaikum $name!\n\nWelcome to Medz with Arfi - Your premium academic hub for MBBS Anatomy & Physiology mastery, guided by Dr. Arfi.\n\nWe are extremely excited to help you excel in your medical studies.\n\nFor direct admin support, educational material requests, or queries, you can contact us 24/7 on WhatsApp: 03246767582 (+923246767582).\n\nBest Regards,\nMedz with Arfi Support Team"
                }
            """.trimIndent()

            val req = Request.Builder()
                .url("https://api.web3forms.com/submit")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(req).execute().use { response ->
                Log.d("EmailSender", "Welcome email automated send code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("EmailSender", "Background welcome email dispatch failed: ", e)
        }
    }
}

data class SearchResults(
    val mcqs: List<MedicalMCQ>,
    val shortQuestions: List<ShortQuestion>,
    val aiExplanation: String
)
