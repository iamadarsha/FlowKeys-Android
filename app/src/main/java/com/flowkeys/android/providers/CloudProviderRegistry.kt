package com.flowkeys.android.providers

import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Optional cloud acceleration interfaces for users who provide their own API keys.
 * Basic offline on-device dictation NEVER requires these.
 */
interface CloudSpeechProvider {
    val providerName: String
    suspend fun transcribe(audio: ByteArray, language: Language): String?
}

interface CloudTextPolisher {
    val providerName: String
    suspend fun polish(rawText: String, language: Language, modePrompt: String): String?
}

class GroqSpeechProvider(private val apiKey: String?) : CloudSpeechProvider {
    override val providerName = "Groq (Whisper-large-v3-turbo)"

    override suspend fun transcribe(audio: ByteArray, language: Language): String? = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) return@withContext null
        try {
            val url = URL("https://api.groq.com/openai/v1/audio/transcriptions")
            val connection = url.openConnection() as HttpURLConnection
            val boundary = "Boundary-" + UUID.randomUUID().toString()
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            val languageCode = when (language) {
                Language.HINDI -> "hi"
                Language.BENGALI -> "bn"
                else -> "en"
            }
            
            val outputStream = connection.outputStream
            val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)
            
            val crlf = "\r\n"
            val twoHyphens = "--"
            
            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"model\"").append(crlf).append(crlf)
            writer.append("whisper-large-v3-turbo").append(crlf)
            
            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"language\"").append(crlf).append(crlf)
            writer.append(languageCode).append(crlf)
            
            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"response_format\"").append(crlf).append(crlf)
            writer.append("json").append(crlf)
            
            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"").append(crlf)
            writer.append("Content-Type: audio/wav").append(crlf).append(crlf)
            writer.flush()
            
            outputStream.write(audio)
            outputStream.flush()
            
            writer.append(crlf).flush()
            writer.append(twoHyphens).append(boundary).append(twoHyphens).append(crlf)
            writer.flush()
            writer.close()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                return@withContext json.optString("text").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}

class GeminiTextProvider(private val apiKey: String?) : CloudTextPolisher {
    override val providerName = "Gemini Flash"

    override suspend fun polish(rawText: String, language: Language, modePrompt: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) return@withContext null
        // HTTP API call to generativelanguage.googleapis.com
        return@withContext null
    }
}

/**
 * Optional Sarvam Saaras v3 cloud speech provider specialized for Indian languages
 * (Bengali, Hindi, Indian English, and code-mixed speech).
 */
class SarvamSpeechProvider(private val apiKey: String?) : CloudSpeechProvider {
    override val providerName = "Sarvam Saaras v3"

    override suspend fun transcribe(audio: ByteArray, language: Language): String? = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) return@withContext null
        try {
            val url = URL("https://api.sarvam.ai/speech-to-text")
            val connection = url.openConnection() as HttpURLConnection
            val boundary = "Boundary-" + UUID.randomUUID().toString()

            connection.requestMethod = "POST"
            connection.setRequestProperty("api-subscription-key", apiKey)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            val languageCode = when (language) {
                Language.HINDI -> "hi-IN"
                Language.BENGALI -> "bn-IN"
                else -> "en-IN"
            }

            val outputStream = connection.outputStream
            val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)
            val crlf = "\r\n"
            val twoHyphens = "--"

            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"model\"").append(crlf).append(crlf)
            writer.append("saaras:v3").append(crlf)

            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"language_code\"").append(crlf).append(crlf)
            writer.append(languageCode).append(crlf)

            writer.append(twoHyphens).append(boundary).append(crlf)
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"").append(crlf)
            writer.append("Content-Type: audio/wav").append(crlf).append(crlf)
            writer.flush()

            outputStream.write(audio)
            outputStream.flush()

            writer.append(crlf).flush()
            writer.append(twoHyphens).append(boundary).append(twoHyphens).append(crlf)
            writer.flush()
            writer.close()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                return@withContext json.optString("transcript").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
