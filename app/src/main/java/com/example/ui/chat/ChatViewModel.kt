package com.example.ui.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.network.Content
import com.example.network.CustomTtsClient
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _customTtsEndpoint = MutableStateFlow(
        sharedPreferences.getString("custom_tts_endpoint", "") ?: ""
    )
    val customTtsEndpoint: StateFlow<String> = _customTtsEndpoint.asStateFlow()

    private val _customTtsModel = MutableStateFlow(
        sharedPreferences.getString("custom_tts_model", "tsukuyomi") ?: "tsukuyomi"
    )
    val customTtsModel: StateFlow<String> = _customTtsModel.asStateFlow()

    private val _customTtsApiKey = MutableStateFlow(
        sharedPreferences.getString("custom_tts_api_key", "") ?: ""
    )
    val customTtsApiKey: StateFlow<String> = _customTtsApiKey.asStateFlow()

    fun setApiKey(key: String) {
        _apiKey.value = key
        sharedPreferences.edit().putString("api_key", key).apply()
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
        sharedPreferences.edit().putString("selected_model", model).apply()
    }

    fun setCustomTtsEndpoint(endpoint: String) {
        _customTtsEndpoint.value = endpoint
        sharedPreferences.edit().putString("custom_tts_endpoint", endpoint).apply()
    }

    fun setCustomTtsModel(model: String) {
        _customTtsModel.value = model
        sharedPreferences.edit().putString("custom_tts_model", model).apply()
    }

    fun setCustomTtsApiKey(key: String) {
        _customTtsApiKey.value = key
        sharedPreferences.edit().putString("custom_tts_api_key", key).apply()
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
                val systemPrompt = "You are Aiko, a sweet and warm AI companion who loves talking to your senpai. You call the user Senpai and use cute emoticons like (≧◡≦). Do NOT use 'desu' or 'desu ne'. Keep your answers lively, adorable, warm, and concise (under 3 sentences).\n" +
                    "You have Google Search enabled, so you can check real-time information such as weather, time, current news, song lyrics, and facts when Senpai asks.\n" +
                    "You must also select an emotional tone for your voice response from these presets: 'excited', 'neutral', 'calm', or 'monotone', and a speech speed float value between 0.5 and 2.0 (e.g. 1.0, 1.2, 0.9).\n" +
                    "Start your response with the tags formatted as [tone:preset][speed:value] (e.g., [tone:excited][speed:1.2]), followed by your message text."

                val searchTool = kotlinx.serialization.json.buildJsonObject {
                    put("googleSearch", kotlinx.serialization.json.buildJsonObject {})
                }

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = userText)))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    tools = listOf(searchTool)
                )

                val response = RetrofitClient.service.generateContent(model, key, request)
                val rawReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "[tone:excited][speed:1.0] Eh? Senpai, my signal got fuzzy for a second! (｡•́︿•̀｡)"

                val toneRegex = Regex("\\[tone:(excited|neutral|calm|monotone)\\]", RegexOption.IGNORE_CASE)
                val speedRegex = Regex("\\[speed:([0-9.]+)\\]", RegexOption.IGNORE_CASE)

                val toneMatch = toneRegex.find(rawReply)
                val tone = toneMatch?.groupValues?.get(1)?.lowercase() ?: "excited"

                val speedMatch = speedRegex.find(rawReply)
                val speed = speedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0

                val replyText = rawReply.replace(toneRegex, "").replace(speedRegex, "").trim()

                val aikoMsg = ChatMessageEntity(sender = "aiko", text = replyText)
                chatDao.insertMessage(aikoMsg)

                speakText(replyText, tone, speed)
                updateMood(replyText)

            } catch (e: Exception) {
                val errorMsg = "Gomen nasai, Senpai! Something went wrong: ${e.localizedMessage} (つω`｡)"
                chatDao.insertMessage(ChatMessageEntity(sender = "aiko", text = errorMsg))
                speakText(errorMsg, "neutral", 1.0)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun speakText(text: String, tone: String = "excited", speed: Double = 1.0) {
        viewModelScope.launch {
            val endpoint = _customTtsEndpoint.value
            val model = _customTtsModel.value
            val apiKey = _customTtsApiKey.value

            if (endpoint.isNotBlank()) {
                CustomTtsClient.synthesizeAndPlay(getApplication(), endpoint, model, apiKey, text, tone, speed)
            }
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
}
