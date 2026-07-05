package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatMessage
import com.example.data.database.ChatMessageDao
import com.example.data.database.Note
import com.example.data.database.NoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(
    private val noteDao: NoteDao,
    private val chatMessageDao: ChatMessageDao
) {
    // --- Room Database (Notes) ---
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Int) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    // --- Room Database (Chat Messages) ---
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        chatMessageDao.clearChatHistory()
    }

    // --- Gemini API (REST Calls) ---
    suspend fun generateAiResponse(
        prompt: String,
        systemInstructionText: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please set the GEMINI_API_KEY environment variable (or GitHub Actions secret) and rebuild the app."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = systemInstructionText?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response received from Gemini."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        }
    }

    suspend fun summarizeNote(noteTitle: String, noteContent: String): String {
        val prompt = """
            Please analyze and summarize the following note. 
            Format the output with:
            1. A 1-sentence concise overview.
            2. 3 key bullet points or actionable insights.
            3. A list of relevant tags.
            
            Note Title: $noteTitle
            Note Content: $noteContent
        """.trimIndent()

        val systemInstruction = "You are an elite, highly organized AI note-taking and productivity assistant."
        return generateAiResponse(prompt, systemInstruction)
    }

    suspend fun polishNote(noteTitle: String, noteContent: String): String {
        val prompt = """
            Please polish and improve the grammar, structure, and professional tone of the following note. 
            Keep the core meaning intact but make it read beautifully and professionally.
            
            Note Title: $noteTitle
            Note Content: $noteContent
        """.trimIndent()

        val systemInstruction = "You are a professional editor and writer who improves content while preserving the original intent."
        return generateAiResponse(prompt, systemInstruction)
    }
}
