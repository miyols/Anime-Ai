package com.example.network

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CustomTtsClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun synthesizeAndPlay(
        context: Context,
        endpointUrl: String,
        modelName: String,
        apiKey: String,
        text: String,
        tone: String = "excited",
        speed: Double = 1.0
    ) {
        if (endpointUrl.isBlank()) {
            Log.w("CustomTtsClient", "Endpoint URL is blank, skipping custom TTS.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val cleanText = text
                    .replace(Regex("[\\p{So}\\p{Cn}]"), "") // Unicode emojis and symbols
                    .replace(Regex("[~*()（）≧◡≦｡•́︿•̀｡つω`｡✨❤️💕💖💗💓💘💞💝⭐🌟💫]"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (cleanText.isBlank()) {
                    Log.w("CustomTtsClient", "Cleaned text is blank.")
                    return@withContext
                }

                val jsonBody = JSONObject().apply {
                    put("model", modelName.ifBlank { "tsukuyomi" })
                    put("input", cleanText)
                    put("speed", speed)
                    put("tone", tone.ifBlank { "excited" })
                }.toString()

                Log.d("CustomTtsClient", "Sending TTS request to: $endpointUrl with payload: $jsonBody")

                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val reqBuilder = Request.Builder()
                    .url(endpointUrl.trim())
                    .post(requestBody)

                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
                }

                val response = client.newCall(reqBuilder.build()).execute()
                Log.d("CustomTtsClient", "Received response code: ${response.code}")

                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        Log.d("CustomTtsClient", "Received ${bytes.size} audio bytes (.wav). Saving to temp file...")
                        val tempFile = File.createTempFile("tts_audio_", ".wav", context.cacheDir)
                        tempFile.writeBytes(bytes)

                        withContext(Dispatchers.Main) {
                            try {
                                MediaPlayer().apply {
                                    setDataSource(tempFile.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        Log.d("CustomTtsClient", "Audio playback completed successfully.")
                                        release()
                                        try { tempFile.delete() } catch (_: Exception) {}
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        Log.e("CustomTtsClient", "MediaPlayer error: what=$what, extra=$extra")
                                        release()
                                        try { tempFile.delete() } catch (_: Exception) {}
                                        true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CustomTtsClient", "Error playing audio file", e)
                                try { tempFile.delete() } catch (_: Exception) {}
                            }
                        }
                    } else {
                        Log.e("CustomTtsClient", "TTS response body is empty or null")
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e("CustomTtsClient", "TTS request failed: ${response.code} ${response.message} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("CustomTtsClient", "TTS exception occurred", e)
            }
        }
    }
}
