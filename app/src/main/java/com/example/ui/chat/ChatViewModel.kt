package com.example.ui.chat

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val sharedPreferences = application.getSharedPreferences("aiko_prefs", Context.MODE_PRIVATE)

    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aikoMood = MutableStateFlow("Happy (≧◡≦)")
    val aikoMood: StateFlow<String> = _aikoMood.asStateFlow()

    private val _apiKey = MutableStateFlow(
        sharedPreferences.getString("api_key", BuildConfig.GEMINI_API_KEY) ?: BuildConfig.GEMINI_API_KEY
    )
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        sharedPreferences.getString("selected_model", "models/gemini-3.5-flash-lite") ?: "models/gemini-3.5-flash-lite"
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(
        listOf(
            "models/gemini-3.5-flash-lite",
            "models/gemini-3.5-flash",
            "models/gemini-1.5-flash",
            "models/gemini-1.5-pro"
        )
    )
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _scanStatus = MutableStateFlow("")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val _voiceStyle = MutableStateFlow(
        sharedPreferences.getString("voice_style", "Japanese Seiyuu (JP Locale + Cute Pitch)") ?: "Japanese Seiyuu (JP Locale + Cute Pitch)"
    )
    val voiceStyle: StateFlow<String> = _voiceStyle.asStateFlow()

    private val _availableVoiceStyles = MutableStateFlow<List<String>>(
        listOf(
            "Japanese Seiyuu (JP Locale + Cute Pitch)",
            "Super High Pitch Kawaii (~desu!)",
            "Tsundere Energetic Voice",
            "Sweet & Soft Anime Voice"
        )
    )
    val availableVoiceStyles: StateFlow<List<String>> = _availableVoiceStyles.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                applyVoiceStyle()
            }
        }
    }

    fun setVoiceStyle(style: String) {
        _voiceStyle.value = style
        sharedPreferences.edit().putString("voice_style", style).apply()
        applyVoiceStyle()
    }

    fun applyVoiceStyle() {
        if (!isTtsInitialized) return
        when (_voiceStyle.value) {
            "Japanese Seiyuu (JP Locale + Cute Pitch)" -> {
                val res = tts?.setLanguage(Locale.JAPAN)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
                tts?.setPitch(1.75f)
                tts?.setSpeechRate(1.15f)
            }
            "Super High Pitch Kawaii (~desu!)" -> {
                tts?.language = Locale.US
                tts?.setPitch(1.95f)
                tts?.setSpeechRate(1.2f)
            }
            "Tsundere Energetic Voice" -> {
                tts?.language = Locale.US
                tts?.setPitch(1.5f)
                tts?.setSpeechRate(1.3f)
            }
            else -> {
                tts?.language = Locale.US
                tts?.setPitch(1.6f)
                tts?.setSpeechRate(1.05f)
            }
        }
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        sharedPreferences.edit().putString("api_key", key).apply()
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
        sharedPreferences.edit().putString("selected_model", model).apply()
    }

    fun scanModels() {
        viewModelScope.launch {
            _scanStatus.value = "Scanning available models..."
            try {
                val key = _apiKey.value
                val response = RetrofitClient.service.listModels(key)
                val modelNames = response.models?.map { it.name }?.filter { it.contains("gemini") } ?: emptyList()
                if (modelNames.isNotEmpty()) {
                    _availableModels.value = modelNames
                    _scanStatus.value = "Successfully scanned ${modelNames.size} models! ✨"
                } else {
                    _scanStatus.value = "No models found or check API key."
                }
            } catch (e: Exception) {
                _scanStatus.value = "Scan error: ${e.localizedMessage}"
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(sender = "user", text = userText)
            chatDao.insertMessage(userMsg)

            _isLoading.value = true

            try {
                val key = _apiKey.value
                val model = _selectedModel.value
                val systemPrompt = "You are Aiko, a super cute, energetic, and sweet anime girl AI companion who loves talking to your senpai. You use cute expressions like ~desu, (≧◡≦), desu ne, and call the user Senpai! Keep your answers lively, adorable, warm, and concise (under 3 sentences)."

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = userText)))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = RetrofitClient.service.generateContent(model, key, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Eh? Senpai, my signal got fuzzy for a second! (｡•́︿•̀｡)"

                val aikoMsg = ChatMessageEntity(sender = "aiko", text = replyText)
                chatDao.insertMessage(aikoMsg)

                speakText(replyText)
                updateMood(replyText)

            } catch (e: Exception) {
                val errorMsg = "Gomen nasai, Senpai! Something went wrong: ${e.localizedMessage} (つω`｡)"
                chatDao.insertMessage(ChatMessageEntity(sender = "aiko", text = errorMsg))
                speakText(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun speakText(text: String) {
        if (isTtsInitialized) {
            val cleanText = text.replace(Regex("[~*()≧◡≦｡•́︿•̀｡つω`｡✨❤️]"), "")
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun updateMood(reply: String) {
        val moods = listOf("Happy (≧◡≦)", "Excited ✨", "Shy (⁄ ⁄•⁄ω⁄•⁄ ⁄)", "Tsundere >_<", "Affectionate ❤️")
        _aikoMood.value = moods.random()
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.clearMessages()
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
