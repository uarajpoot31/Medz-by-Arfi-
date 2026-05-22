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

    // User Profile
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    // --- VIEW STATES ---
    var selectedTab by mutableStateOf("dashboard") // dashboard, study, quiz, notes, search, assistant, admin, profile

    // Auth Simulation (Secured in User Profile Room Database)
    var isLoggedIn by mutableStateOf(false)
    var authEmail by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authName by mutableStateOf("")
    var isSignUpMode by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)

    // Search Engine
    var searchText by mutableStateOf("")
    var searchResults by mutableStateOf<SearchResults?>(null)
    var isSearching by mutableStateOf(false)

    // AI Q&A Assistant
    var aiQueryText by mutableStateOf("")
    var chatHistory = mutableStateListOf<ChatMessage>()
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
        if (authEmail.isBlank() || authPassword.isBlank()) {
            authError = "Please enter both Email and Password."
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
        // Save to Room Profile as real authenticated info
        viewModelScope.launch {
            val shortName = authEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            repository.updateProfile(
                UserProfile(
                    id = "primary_user",
                    name = if (authName.isNotBlank()) authName else "Dr. $shortName",
                    email = authEmail
                )
            )
            isLoggedIn = true
            authError = null
            selectedTab = "dashboard"
        }
    }

    fun performSignUp() {
        if (authName.isBlank() || authEmail.isBlank() || authPassword.isBlank()) {
            authError = "All fields are required during signup."
            return
        }
        performLogin()
    }

    fun triggerForgotPassword() {
        authError = "Password recovery link has been safely routed to $authEmail (Simulated)"
    }

    fun performSignOut() {
        isLoggedIn = false
        authEmail = ""
        authPassword = ""
        authName = ""
        isSignUpMode = false
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            repository.updateProfile(
                UserProfile(
                    id = "primary_user",
                    name = "Dr. Google Scholar",
                    email = "scholar.google@medzwitharfi.edu"
                )
            )
            isLoggedIn = true
            selectedTab = "dashboard"
        }
    }

    // --- SEARCH METHODS ---
    fun executeSearch() {
        if (searchText.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            try {
                val results = repository.performSmartSearch(searchText, allMCQs.value)
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
            repository.saveQuizScore(score)
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
