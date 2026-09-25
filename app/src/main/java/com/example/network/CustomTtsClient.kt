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
        text: String
    ) {
        if (endpointUrl.isBlank()) {
            Log.w("CustomTtsClient", "Endpoint URL is blank, skipping custom TTS.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val cleanText = text.replace(Regex("[~*()≧◡≦｡•́︿•̀｡つω`｡✨❤️]"), "")
                if (cleanText.isBlank()) return@withContext

                val jsonBody = JSONObject().apply {
                    put("model", modelName.ifBlank { "tsukuyomi" })
                    put("input", cleanText)
                }.toString()

                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val reqBuilder = Request.Builder()
                    .url(endpointUrl.trim())
                    .post(requestBody)

                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
                }

                val response = client.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        // Save audio bytes to temp file with .wav extension and play
                        val tempFile = File.createTempFile("tts_audio_", ".wav", context.cacheDir)
                        tempFile.writeBytes(bytes)

                        withContext(Dispatchers.Main) {
                            try {
                                MediaPlayer().apply {
                                    setDataSource(tempFile.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
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
                                Log.e("CustomTtsClient", "Error playing audio", e)
                                try { tempFile.delete() } catch (_: Exception) {}
                            }
                        }
                    } else {
                        Log.e("CustomTtsClient", "TTS response body is empty")
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e("CustomTtsClient", "TTS request failed: ${response.code} ${response.message} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("CustomTtsClient", "TTS exception", e)
            }
        }
    }
}
