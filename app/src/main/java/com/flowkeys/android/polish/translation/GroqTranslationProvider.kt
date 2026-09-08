package com.flowkeys.android.polish.translation

import android.util.Log
import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GroqTranslationProvider {
    private const val TAG = "GroqTranslationProvider"
    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    
    // Ordered candidate models currently active on Groq
    private val CANDIDATE_MODELS = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "gemma2-9b-it"
    )
    
    suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank() || apiKey.isBlank()) return@withContext null
        
        for (model in CANDIDATE_MODELS) {
            val result = tryTranslateWithModel(text, sourceLang, targetLang, apiKey, model)
            if (!result.isNullOrBlank()) {
                return@withContext result
            }
        }
        null
    }

    private fun tryTranslateWithModel(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        apiKey: String,
        modelName: String
    ): String? {
        try {
            Log.d(TAG, "Translating via Groq ($modelName): [${text.length} chars, ${sourceLang.code}→${targetLang.code}]")
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.connectTimeout = 6000
            connection.readTimeout = 10000
            connection.doOutput = true

            val systemPrompt = "You are a professional, direct speech-to-text translator for chat messages. Translate the input speech from ${sourceLang.displayName} directly to natural ${targetLang.displayName}. The speech may contain code-mixing (Benglish, Hinglish, loanwords) - translate ALL of it accurately into natural ${targetLang.displayName}. CRITICAL INSTRUCTION: Output ONLY the translated sentence. Never include explanations, notes, punctuation markup, or quotes."

            val jsonPayload = JSONObject().apply {
                put("model", modelName)
                put("temperature", 0.2)
                put("max_tokens", 500)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    })
                }
                put("messages", messages)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Groq response ($modelName): HTTP 200, [${responseString.length} chars]")
                val json = JSONObject(responseString)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")?.trim()
                    if (!content.isNullOrBlank()) {
                        // Strip <think>...</think> tags if model produces thinking reasoning
                        var cleaned = content
                        if (cleaned.contains("</think>")) {
                            cleaned = cleaned.substringAfter("</think>").trim()
                        }
                        cleaned = cleaned.removeSurrounding("\"").removeSurrounding("'").trim()
                        Log.i(TAG, "Translated result ($modelName): '$cleaned'")
                        return cleaned
                    }
                }
            } else {
                val errorString = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "Groq model $modelName returned HTTP $responseCode: $errorString")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Translation failed on model $modelName: ${e.message}")
        }
        return null
    }
}
