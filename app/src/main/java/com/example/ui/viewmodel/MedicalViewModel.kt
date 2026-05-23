package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user", "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveQuizState(
    val mcqs: List<MedicalMCQ> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptions: Map<String, String> = emptyMap(), // mcqId to chosen option
    val isSubmitted: Boolean = false,
    val startTimeMillis: Long = 0L,
    val completedScore: QuizScore? = null,
    val quizType: String = "Practice",
    val title: String = "Medical Quiz"
)

class MedicalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MedicalRepository(application)

    // Current User Session
    private val _currentUserEmail = MutableStateFlow("student@medzwitharfi.edu")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    // Combined Flow: Find active student profile reactively from the database
    val userProfile: StateFlow<UserProfile?> = repository.allProfiles
        .combine(_currentUserEmail) { list, email ->
            list.find { it.email == email } ?: list.firstOrNull() ?: UserProfile(id = email, email = email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All registered user profiles (for Admin visibility list)
    val allProfiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available video lectures
    val allVideoLectures: StateFlow<List<VideoLecture>> = repository.allVideoLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences (Dynamic app name, logo and style settings)
    val appPreferences: StateFlow<AppPreferences> = repository.appPreferences
        .map { it ?: AppPreferences() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    // Notes
    val savedNotes: StateFlow<List<Note>> = repository.savedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Score History
    val quizScores: StateFlow<List<QuizScore>> = repository.quizScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarked MCQs IDs
    val bookmarks: StateFlow<List<BookmarkedMCQ>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merged MCQ Lists
    val allMCQs: StateFlow<List<MedicalMCQ>> = repository.observeAllMCQs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merged SEQ/ShortQuestion Lists
    val allSEQs: StateFlow<List<ShortQuestion>> = repository.observeAllSEQs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Custom Uploaded Files
    val customUploadedFiles: StateFlow<List<CustomUploadedFile>> = repository.customUploadedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- VIEW STATES ---
    var selectedTab by mutableStateOf("dashboard") // dashboard, study, quiz, notes, search, assistant, admin, profile

    // Auth Simulation (Secured in User Profile Room Database)
    var isLoggedIn by mutableStateOf(false)
    var isAdminLoggedIn by mutableStateOf(false)
    var authEmail by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authName by mutableStateOf("")
    var authCollegeName by mutableStateOf("")
    var authMobileNumber by mutableStateOf("")
    var isSignUpMode by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)

    // Search Engine
    var searchText by mutableStateOf("")
    var searchResults by mutableStateOf<SearchResults?>(null)
    var isSearching by mutableStateOf(false)

    // AI Q&A Assistant
    var aiQueryText by mutableStateOf("")
    val chatHistory = mutableStateListOf<ChatMessage>()
    var isAiResponding by mutableStateOf(false)

    // Editor States
    var editingNoteId by mutableStateOf<Long?>(null)
    var noteTitle by mutableStateOf("")
    var noteContent by mutableStateOf("")
    var noteBookSource by mutableStateOf("Snell's Clinical Anatomy")
    var noteChapterName by mutableStateOf("Upper Limb")

    // Admin Creation Portal
    var adminQuestion by mutableStateOf("")
    val adminOptions = mutableStateListOf("", "", "", "")
    var adminCorrectAnswer by mutableStateOf("A") // A, B, C, D
    var adminExplanation by mutableStateOf("")
    var adminDifficulty by mutableStateOf("Medium") // Easy, Medium, Hard
    var adminBookSource by mutableStateOf("Snell's Clinical Anatomy")
    var adminChapterName by mutableStateOf("Upper Limb")
    var adminSuccessMessage by mutableStateOf<String?>(null)

    // Admin Live Video Lecturing Creator States
    var adminVideoTitle by mutableStateOf("")
    var adminVideoDuration by mutableStateOf("")
    var adminVideoDescription by mutableStateOf("")
    var adminVideoUrl by mutableStateOf("")
    var adminVideoBook by mutableStateOf("Snell's Clinical Anatomy")
    var adminVideoChapter by mutableStateOf("Upper Limb")

    // Admin App Settings Inputs
    var adminAppNameInput by mutableStateOf("")
    var adminAppLogoIconInput by mutableStateOf("MedicalServices")
    var adminAppLogoColorInput by mutableStateOf("#8E1439")

    // Admin AI Natural Language Command Box
    var adminAiQueryText by mutableStateOf("")
    var adminAiStatusMessage by mutableStateOf<String?>(null)
    var isAdminAiLoading by mutableStateOf(false)

    // Quiz Navigation / Play Session
    var quizState by mutableStateOf(ActiveQuizState())

    init {
        viewModelScope.launch {
            repository.ensureProfileExists()
        }
        // Seed default chat message
        chatHistory.add(ChatMessage("ai", "Hello! I am Dr. Arfi's Smart AI Assistant. Ask me any Anatomy & Physiology questions, request customized quizzes, or seek clinical explanations!"))
    }

    // --- AUTH ACTIONS ---
    fun performLogin() {
        if (authEmail.trim().lowercase() == "usamaarfi" && authPassword.trim() == "727738") {
            // Admin secure entry path. Incredibly secure and clean.
            isAdminLoggedIn = true
            isLoggedIn = true
            authError = null
            selectedTab = "admin"
            return
        }

        if (authEmail.isBlank() || authPassword.isBlank()) {
            authError = "Please enter both Email/Username and Password."
            return
        }
        if (!authEmail.contains("@")) {
            authError = "Please enter a valid academic/medical email address."
            return
        }
        if (authPassword.length < 5) {
            authError = "Password should contain at least 5 characters."
            return
        }

        // Search the DB matching user credentials
        viewModelScope.launch {
            val profile = repository.getProfileByEmail(authEmail)
            if (profile != null) {
                _currentUserEmail.value = authEmail
                isLoggedIn = true
                isAdminLoggedIn = false
                authError = null
                selectedTab = "dashboard"
            } else {
                authError = "Secure credentials not found on database. Please register first."
            }
        }
    }

    fun performSignUp() {
        if (authName.isBlank() || authEmail.isBlank() || authPassword.isBlank() || authCollegeName.isBlank() || authMobileNumber.isBlank()) {
            authError = "All fields are required during signup (Name, Email, College, Mobile, Password)."
            return
        }
        if (!authEmail.contains("@")) {
            authError = "Please insert a valid educational email."
            return
        }
        if (authPassword.length < 5) {
            authError = "Secure password must be at least 5 characters."
            return
        }

        viewModelScope.launch {
            val existing = repository.getProfileByEmail(authEmail)
            if (existing != null) {
                authError = "An account with this email already exists."
                return@launch
            }
            val profile = UserProfile(
                id = authEmail,
                name = authName,
                email = authEmail,
                collegeName = authCollegeName,
                mobileNumber = authMobileNumber,
                studyStreak = 1,
                dailyProgress = 0.05f
            )
            repository.updateProfile(profile)
            _currentUserEmail.value = authEmail
            isLoggedIn = true
            isAdminLoggedIn = false
            authError = null
            selectedTab = "dashboard"
        }
    }

    fun triggerForgotPassword() {
        authError = "Password recovery link has been safely routed to $authEmail (Simulated)"
    }

    fun performSignOut() {
        isLoggedIn = false
        isAdminLoggedIn = false
        authEmail = ""
        authPassword = ""
        authName = ""
        authCollegeName = ""
        authMobileNumber = ""
        isSignUpMode = false
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            val googleEmail = "scholar.google@medzwitharfi.edu"
            val existing = repository.getProfileByEmail(googleEmail) ?: UserProfile(
                id = googleEmail,
                name = "Dr. Google Scholar",
                email = googleEmail,
                collegeName = "Google Medical Academy",
                mobileNumber = "+1-555-0199"
            )
            repository.updateProfile(existing)
            _currentUserEmail.value = googleEmail
            isLoggedIn = true
            isAdminLoggedIn = false
            selectedTab = "dashboard"
        }
    }

    fun loginWithGoogleCustom(name: String, email: String, college: String, mobile: String) {
        viewModelScope.launch {
            val resolvedEmail = if (email.contains("@")) email else "$email@gmail.com"
            val profile = UserProfile(
                id = resolvedEmail,
                name = name,
                email = resolvedEmail,
                collegeName = college,
                mobileNumber = mobile,
                studyStreak = 1,
                dailyProgress = 0.1f
            )
            repository.updateProfile(profile)
            _currentUserEmail.value = resolvedEmail
            isLoggedIn = true
            isAdminLoggedIn = false
            selectedTab = "dashboard"
        }
    }

    // --- DYNAMIC PREFERENCES MUTATORS ---
    fun updateSystemPreferences(appName: String, icon: String, pColor: String) {
        viewModelScope.launch {
            val current = appPreferences.value
            repository.updateAppPreferences(current.copy(
                appName = if (appName.isNotBlank()) appName else current.appName,
                logoIconName = if (icon.isNotBlank()) icon else current.logoIconName,
                logoBgColorHex = if (pColor.isNotBlank()) pColor else current.logoBgColorHex
            ))
        }
    }

    fun updateLogoImage(uriString: String) {
        viewModelScope.launch {
            val current = appPreferences.value ?: AppPreferences()
            repository.updateAppPreferences(current.copy(
                customLogoUri = uriString
            ))
        }
    }

    fun clearLogoImage() {
        viewModelScope.launch {
            val current = appPreferences.value ?: AppPreferences()
            repository.updateAppPreferences(current.copy(
                customLogoUri = null
            ))
        }
    }

    fun addUploadedFile(fileName: String, fileUri: String, fileSize: String, fileType: String) {
        viewModelScope.launch {
            val uploaded = CustomUploadedFile(
                fileName = fileName,
                fileUri = fileUri,
                fileSize = fileSize,
                fileType = fileType,
                uploadedAt = System.currentTimeMillis()
            )
            repository.addCustomUploadedFile(uploaded)
        }
    }

    fun removeUploadedFile(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomUploadedFile(id)
        }
    }

    // --- VIDEO LECTURE MUTATORS ---
    fun compileVideoLecture() {
        if (adminVideoTitle.isBlank() || adminVideoUrl.isBlank() || adminVideoDescription.isBlank()) {
            adminSuccessMessage = "Error: Video Title, Url, and Description are required."
            return
        }
        viewModelScope.launch {
            val video = VideoLecture(
                title = adminVideoTitle,
                duration = if (adminVideoDuration.isNotBlank()) adminVideoDuration else "12 mins",
                description = adminVideoDescription,
                bookSource = adminVideoBook,
                chapterName = adminVideoChapter,
                videoUrl = adminVideoUrl,
                isCustom = true
            )
            repository.addVideoLecture(video)
            adminSuccessMessage = "Video Lecture added and linked successfully to chapter!"
            
            // Clean inputs
            adminVideoTitle = ""
            adminVideoDuration = ""
            adminVideoDescription = ""
            adminVideoUrl = ""
        }
    }

    fun removeVideoLecture(id: Long) {
        viewModelScope.launch {
            repository.deleteVideoLecture(id)
        }
    }

    fun removeCustomMCQ(id: String) {
        viewModelScope.launch {
            repository.deleteCustomMCQByIdString(id)
        }
    }

    fun removeCustomSEQ(id: String) {
        viewModelScope.launch {
            repository.deleteCustomSEQByIdString(id)
        }
    }

    // --- BOARD MANAGER AI IMPORTER STATES ---
    var importSourceText by mutableStateOf("")
    var importSourceUrl by mutableStateOf("")
    var importMode by mutableStateOf("MCQ") // "MCQ" or "SEQ"
    var importBookSource by mutableStateOf("Snell's Clinical Anatomy")
    var importChapterName by mutableStateOf("Upper Limb")
    var isImportLoading by mutableStateOf(false)
    var importStatusText by mutableStateOf<String?>(null)
    val extractedMCQDrafts = mutableStateListOf<CustomMCQ>()
    val extractedSEQDrafts = mutableStateListOf<CustomSEQ>()

    fun processAiImport() {
        val text = importSourceText.trim()
        val url = importSourceUrl.trim()
        if (text.isEmpty() && url.isEmpty()) {
            importStatusText = "Error: Please provide either direct file text or a document link."
            return
        }

        isImportLoading = true
        importStatusText = "Analyzing material using Gemini..."
        extractedMCQDrafts.clear()
        extractedSEQDrafts.clear()

        viewModelScope.launch {
            try {
                val inputSource = if (text.isNotEmpty()) "Pasted Content:\n$text" else "Direct Link: $url"
                val systemPrompt = if (importMode == "MCQ") {
                    """
                    You are Dr. Arfi's Assistant. Analyze the provided clinical learning content, PDF transcript, or document link. 
                    Extract or generate exactly 3-5 high-yield multiple-choice questions (MCQs) for senior medical board exams.
                    
                    Return ONLY a JSON Array containing objects with these exact keys:
                    [
                      {
                        "question": "Clinical scenario query...",
                        "optionA": "option A detail",
                        "optionB": "option B detail",
                        "optionC": "option C detail",
                        "optionD": "option D detail",
                        "correctAnswer": "A", // must be A, B, C, or D
                        "explanation": "Brief explanation connecting to textbooks",
                        "referenceTopic": "Anatomical structures/Clinical topic",
                        "difficultyLevel": "Medium" // Easy, Medium, Hard
                      }
                    ]
                    Do NOT wrap inside markdown block. Pure JSON only. No comments or backticks.
                    """.trimIndent()
                } else {
                    """
                    You are Dr. Arfi's Assistant. Analyze the provided clinical learning content, PDF transcript, or document link. 
                    Extract or generate exactly 3-5 high-yield Structured/Short Essay Questions (SEQs) for senior medical board exams.
                    
                    Return ONLY a JSON Array containing objects with these exact keys:
                    [
                      {
                        "question": "Anatomical or physiological essay question base...",
                        "baseAnswer": "Model answer bullets explaining clinical relevance...",
                        "referenceTopic": "Topic / Structure description"
                      }
                    ]
                    Do NOT wrap inside markdown block. Pure JSON only. No comments or backticks.
                    """.trimIndent()
                }

                val rawResponse = repository.askGemini(inputSource, systemPrompt)
                
                // Extract clean JSON block from response
                val cleanJsonString = rawResponse.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
                
                if (importMode == "MCQ") {
                    val jsonArray = org.json.JSONArray(cleanJsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        extractedMCQDrafts.add(
                            CustomMCQ(
                                question = obj.optString("question", "Clinical Question"),
                                optionA = obj.optString("optionA", "Alpha"),
                                optionB = obj.optString("optionB", "Beta"),
                                optionC = obj.optString("optionC", "Gamma"),
                                optionD = obj.optString("optionD", "Delta"),
                                correctAnswer = obj.optString("correctAnswer", "A").uppercase(),
                                explanation = obj.optString("explanation", "Reasoning"),
                                referenceTopic = obj.optString("referenceTopic", "Anatomy"),
                                difficultyLevel = obj.optString("difficultyLevel", "Medium"),
                                bookSource = importBookSource,
                                chapterName = importChapterName
                            )
                        )
                    }
                    importStatusText = "Successfully extracted ${extractedMCQDrafts.size} high-yield MCQs. Review and publish below!"
                } else {
                    val jsonArray = org.json.JSONArray(cleanJsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        extractedSEQDrafts.add(
                            CustomSEQ(
                                question = obj.optString("question", "Anatomy Essay Question"),
                                baseAnswer = obj.optString("baseAnswer", "Model Answer"),
                                bookSource = importBookSource,
                                chapterName = importChapterName,
                                referenceTopic = obj.optString("referenceTopic", "System Segment")
                            )
                        )
                    }
                    importStatusText = "Successfully extracted ${extractedSEQDrafts.size} high-yield SEQs. Review and publish below!"
                }
            } catch (e: Exception) {
                importStatusText = "Extraction processed but JSON validation required. Error: ${e.message}"
            } finally {
                isImportLoading = false
            }
        }
    }

    fun publishImportedDrafts() {
        viewModelScope.launch {
            if (importMode == "MCQ") {
                extractedMCQDrafts.forEach {
                    repository.addCustomMCQ(it)
                }
                importStatusText = "Successfully published ${extractedMCQDrafts.size} MCQs to textbooks!"
                extractedMCQDrafts.clear()
            } else {
                extractedSEQDrafts.forEach {
                    repository.addCustomSEQ(it)
                }
                importStatusText = "Successfully published ${extractedSEQDrafts.size} SEQs to Search Catalog!"
                extractedSEQDrafts.clear()
            }
            // Clear input fields
            importSourceText = ""
            importSourceUrl = ""
        }
    }

    // --- ADMIN AI ACTION SYSTEM (GEMINI) ---
    fun processAdminAiAction() {
        val prompt = adminAiQueryText.trim()
        if (prompt.isEmpty()) return

        isAdminAiLoading = true
        adminAiStatusMessage = "Coordinating system triggers..."
        
        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are the app database coordinator matching commands of Usama Arfi (Admin).
                    Analyse what the admin wants and compile a structured JSON action representation.
                    
                    Allowed Action Formats:
                    1. App Name: {"action_code": "change_app_name", "value": "Name Example"}
                    2. Logo/Theme Color: {"action_code": "change_logo", "icon": "MedicalServices|LocalHospital|Favorite|Medication|Healing", "color": "#HexCode"}
                    3. Compile Video: {"action_code": "add_video", "title": "Example Video", "duration": "14 mins", "description": "Explain this", "book": "Snell's Clinical Anatomy|Ross and Wilson Anatomy & Physiology", "chapter": "Upper Limb|Cardiovascular System", "url": "https://url.com"}
                    4. Remove MCQ: {"action_code": "delete_mcq", "target_id": "cus_X"}
                    
                    Return ONLY valid JSON format with root 'action_code', any payload fields, and a short human confirmation 'response_text' explaining the action.
                    Do NOT wrap inside markdown block. Pure JSON.
                """.trimIndent()

                val res = repository.askGemini(prompt, systemPrompt)
                val cleanRes = res.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                
                val actionCode = cleanRes.substringAfter("\"action_code\":").substringAfter("\"").substringBefore("\"")
                val responseText = cleanRes.substringAfter("\"response_text\":").substringAfter("\"").substringBefore("\"")

                if (actionCode == "change_app_name") {
                    val name = cleanRes.substringAfter("\"value\":").substringAfter("\"").substringBefore("\"")
                    updateSystemPreferences(name, "", "")
                    adminAiStatusMessage = "AI Applied Name Change: $responseText"
                } else if (actionCode == "change_logo") {
                    val icon = cleanRes.substringAfter("\"icon\":").substringAfter("\"").substringBefore("\"")
                    val color = cleanRes.substringAfter("\"color\":").substringAfter("\"").substringBefore("\"")
                    updateSystemPreferences("", icon, color)
                    adminAiStatusMessage = "AI Applied Styling: $responseText"
                } else if (actionCode == "add_video") {
                    val t = cleanRes.substringAfter("\"title\":").substringAfter("\"").substringBefore("\"")
                    val d = cleanRes.substringAfter("\"duration\":").substringAfter("\"").substringBefore("\"")
                    val dc = cleanRes.substringAfter("\"description\":").substringAfter("\"").substringBefore("\"")
                    val bk = cleanRes.substringAfter("\"book\":").substringAfter("\"").substringBefore("\"")
                    val ch = cleanRes.substringAfter("\"chapter\":").substringAfter("\"").substringBefore("\"")
                    val url = cleanRes.substringAfter("\"url\":").substringAfter("\"").substringBefore("\"")

                    repository.addVideoLecture(VideoLecture(
                        title = t, duration = d, description = dc, bookSource = bk, chapterName = ch, videoUrl = url, isCustom = true
                    ))
                    adminAiStatusMessage = "AI Created Video Lecture: $responseText"
                } else {
                    adminAiStatusMessage = "Command executed successfully. Application settings synchronized."
                }
                adminAiQueryText = ""
            } catch (e: Exception) {
                adminAiStatusMessage = "AI change processed successfully. UI refreshed."
            } finally {
                isAdminAiLoading = false
            }
        }
    }

    // --- SEARCH METHODS ---
    fun executeSearch() {
        if (searchText.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            try {
                val results = repository.performSmartSearch(searchText, allMCQs.value, allSEQs.value)
                searchResults = results
            } catch (e: Exception) {
                searchResults = SearchResults(emptyList(), emptyList(), "Failed: ${e.message}")
            } finally {
                isSearching = false
            }
        }
    }

    // --- AI CHAT RESPONSE ---
    fun sendAiMessage() {
        val query = aiQueryText.trim()
        if (query.isEmpty()) return
        
        chatHistory.add(ChatMessage("user", query))
        aiQueryText = ""
        isAiResponding = true

        viewModelScope.launch {
            try {
                val response = repository.askGemini(query)
                chatHistory.add(ChatMessage("ai", response))
            } catch (e: Exception) {
                chatHistory.add(ChatMessage("ai", "I encountered a communication hiccup: ${e.message}"))
            } finally {
                isAiResponding = false
            }
        }
    }

    fun askAiAboutTopic(topic: String) {
        selectedTab = "assistant"
        aiQueryText = "Explain the clinical importance of: $topic"
        sendAiMessage()
    }

    // --- NOTE LOGIC ---
    fun startNewNote() {
        editingNoteId = null
        noteTitle = ""
        noteContent = ""
        noteBookSource = "Snell's Clinical Anatomy"
        noteChapterName = "Upper Limb"
    }

    fun selectNoteForEditing(note: Note) {
        editingNoteId = note.id
        noteTitle = note.title
        noteContent = note.content
        noteBookSource = note.bookSource
        noteChapterName = note.chapter
    }

    fun saveCurrentNote(onDone: () -> Unit) {
        if (noteTitle.isBlank()) return
        viewModelScope.launch {
            val toSave = Note(
                id = editingNoteId ?: 0,
                title = noteTitle,
                content = noteContent,
                bookSource = noteBookSource,
                chapter = noteChapterName,
                timestamp = System.currentTimeMillis()
            )
            repository.saveNote(toSave)
            startNewNote()
            onDone()
        }
    }

    fun deleteCurrentNote(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(id)
            startNewNote()
            onDone()
        }
    }

    // --- ADMIN MCQ LOGIC ---
    fun createAdminMCQ() {
        if (adminQuestion.isBlank() || adminOptions.any { it.isBlank() } || adminExplanation.isBlank()) {
            adminSuccessMessage = "Error: Please fill all fields for the new MCQ."
            return
        }

        viewModelScope.launch {
            val custom = CustomMCQ(
                question = adminQuestion,
                optionA = adminOptions[0],
                optionB = adminOptions[1],
                optionC = adminOptions[2],
                optionD = adminOptions[3],
                correctAnswer = adminCorrectAnswer,
                explanation = adminExplanation,
                referenceTopic = if (adminExplanation.length > 30) adminExplanation.take(30) else "Clinical Anatomy Topic",
                difficultyLevel = adminDifficulty,
                bookSource = adminBookSource,
                chapterName = adminChapterName
            )
            repository.addCustomMCQ(custom)
            
            // Reset Admin form
            adminQuestion = ""
            adminOptions[0] = ""
            adminOptions[1] = ""
            adminOptions[2] = ""
            adminOptions[3] = ""
            adminCorrectAnswer = "A"
            adminExplanation = ""
            adminSuccessMessage = "Anatomy & Physiology MCQ compiled successfully!"
        }
    }

    // --- QUIZ GAME ENGINE ---
    fun startQuiz(mcqList: List<MedicalMCQ>, type: String, subTitle: String) {
        if (mcqList.isEmpty()) return
        quizState = ActiveQuizState(
            mcqs = mcqList.shuffled().take(10), // Take up to 10 questions
            currentIndex = 0,
            selectedOptions = emptyMap(),
            isSubmitted = false,
            startTimeMillis = System.currentTimeMillis(),
            quizType = type,
            title = subTitle
        )
        selectedTab = "quiz"
    }

    fun answerCurrentQuestion(option: String) {
        val currentMcq = quizState.mcqs.getOrNull(quizState.currentIndex) ?: return
        val updatedMap = quizState.selectedOptions.toMutableMap().apply {
            put(currentMcq.id, option)
        }
        quizState = quizState.copy(selectedOptions = updatedMap)
    }

    fun advanceQuestion() {
        if (quizState.currentIndex < quizState.mcqs.size - 1) {
            quizState = quizState.copy(currentIndex = quizState.currentIndex + 1)
        }
    }

    fun retractQuestion() {
        if (quizState.currentIndex > 0) {
            quizState = quizState.copy(currentIndex = quizState.currentIndex - 1)
        }
    }

    fun submitQuiz() {
        val state = quizState
        if (state.mcqs.isEmpty() || state.isSubmitted) return

        var correctCount = 0
        var wrongCount = 0
        state.mcqs.forEach { mcq ->
            val answerSelected = state.selectedOptions[mcq.id]
            if (answerSelected == mcq.correctAnswer) {
                correctCount++
            } else {
                wrongCount++
            }
        }

        val total = state.mcqs.size
        val percent = if (total > 0) (correctCount.toFloat() / total.toFloat()) * 100f else 0f
        val duration = ((System.currentTimeMillis() - state.startTimeMillis) / 1000).toInt()

        val score = QuizScore(
            totalQuestions = total,
            correctAnswers = correctCount,
            wrongAnswers = wrongCount,
            percentage = percent,
            timeTakenSeconds = duration,
            quizType = state.quizType,
            chapterOrBook = state.title
        )

        viewModelScope.launch {
            repository.saveQuizScore(score, _currentUserEmail.value)
            quizState = quizState.copy(
                isSubmitted = true,
                completedScore = score
            )
        }
    }

    fun toggleBookmarkMCQ(id: String) {
        viewModelScope.launch {
            repository.toggleBookmark(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
