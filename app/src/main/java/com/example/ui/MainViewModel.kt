package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ChatMessage
import com.example.data.database.Note
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    NOTES, CHAT
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(Screen.NOTES)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Notes State ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Reactive list of notes filtered by search query and category
    val notes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        searchQuery,
        selectedCategory
    ) { allNotes, query, category ->
        allNotes.filter { note ->
            val matchesQuery = note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || note.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectNote(note: Note?) {
        _activeNote.value = note
    }

    fun saveNote(title: String, content: String, category: String, existingNoteId: Int? = null) {
        viewModelScope.launch {
            val noteToSave = if (existingNoteId != null) {
                Note(
                    id = existingNoteId,
                    title = title,
                    content = content,
                    category = category,
                    aiSummary = _activeNote.value?.aiSummary // preserve AI summary if editing
                )
            } else {
                Note(
                    title = title,
                    content = content,
                    category = category
                )
            }
            repository.insertNote(noteToSave)
            // If editing the active note, refresh activeNote state
            if (_activeNote.value?.id == existingNoteId) {
                _activeNote.value = noteToSave
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNote(id)
            if (_activeNote.value?.id == id) {
                _activeNote.value = null
            }
        }
    }

    // --- Note AI Features ---
    fun runNoteSummary(note: Note) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val summary = repository.summarizeNote(note.title, note.content)
            val updatedNote = note.copy(aiSummary = summary)
            repository.insertNote(updatedNote)
            _activeNote.value = updatedNote
            _isAiLoading.value = false
        }
    }

    fun runNotePolish(note: Note) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val polishedContent = repository.polishNote(note.title, note.content)
            val updatedNote = note.copy(content = polishedContent)
            repository.insertNote(updatedNote)
            _activeNote.value = updatedNote
            _isAiLoading.value = false
        }
    }

    fun clearNoteSummary(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(aiSummary = null)
            repository.insertNote(updatedNote)
            _activeNote.value = updatedNote
        }
    }

    // --- Chat State ---
    val chatInputText = MutableStateFlow("")
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendChatMessage() {
        val queryText = chatInputText.value.trim()
        if (queryText.isEmpty()) return

        chatInputText.value = ""

        viewModelScope.launch {
            // 1. Insert user message
            val userMsg = ChatMessage(sender = "user", text = queryText)
            repository.insertChatMessage(userMsg)

            _isChatLoading.value = true

            // 2. Prepare contextual system instructions and context
            val contextNotesText = notes.value.take(5).joinToString("\n\n") {
                "[Note Category: ${it.category}] Title: ${it.title}\nContent: ${it.content}"
            }
            val systemInstruction = """
                You are "AI Assistant", a highly specialized personal productivity, journaling, and brainstorming co-pilot.
                You are helpful, structured, and insightful.
                Below are the user's recent personal notes to help you give highly contextual and tailored advice when they ask about their notes, schedules, ideas, or work:
                
                $contextNotesText
                
                Always refer to these notes naturally if relevant, but do not hallucinate information. If the user asks general questions, answer them cleanly and concisely.
            """.trimIndent()

            // 3. Generate response
            val replyText = repository.generateAiResponse(queryText, systemInstruction)

            // 4. Insert assistant message
            val geminiMsg = ChatMessage(sender = "gemini", text = replyText)
            repository.insertChatMessage(geminiMsg)

            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun sendQuickPrompt(promptText: String) {
        chatInputText.value = promptText
        sendChatMessage()
    }
}

class ViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
