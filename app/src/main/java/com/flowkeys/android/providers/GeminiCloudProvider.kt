package com.flowkeys.android.providers

import android.util.Base64
import android.util.Log
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.modes.SmartMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Gemini Cloud Provider orchestrating frontier-grade Multimodal Speech Transcription
 * and Language Intelligence / Translation for FlowKeys.
 *
 * Models:
 * - gemini-2.0-flash (Primary - frontier latency and multilingual intelligence)
 * - gemini-1.5-flash (Secondary fallback)
 */
object GeminiCloudProvider {

    private const val TAG = "GeminiCloudProvider"

    // 2026 Live Supported Models (Prioritized by quota, latency, and capability)
    // 1. gemini-3.5-flash-lite: 15 RPM, 500 RPD (Top choice for real-time mobile typing: fast & high daily quota)
    // 2. gemini-3.1-flash-lite: 15 RPM, 500 RPD (Secondary high-quota fallback)
    // 3. gemini-3.8-flash: 5 RPM, 20 RPD (Frontier capability fallback)
    // 4. gemini-2.5-flash: 5 RPM, 20 RPD
    private val CANDIDATE_MODELS = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3.8-flash",
        "gemini-2.5-flash"
    )

    /**
     * Transcribes raw 16kHz PCM WAV audio directly using Gemini's native multimodal understanding.
     */
    suspend fun transcribe(
        audioWav: ByteArray,
        language: Language,
        apiKey: String,
        vocabularyHints: List<String> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        if (audioWav.isEmpty() || apiKey.isBlank()) return@withContext null

        val base64Audio = Base64.encodeToString(audioWav, Base64.NO_WRAP)
        val prompt = buildTranscriptionPrompt(language, vocabularyHints)

        for (model in CANDIDATE_MODELS) {
            val result = tryTranscribeWithModel(model, base64Audio, prompt, apiKey)
            if (!result.isNullOrBlank()) {
                return@withContext result
            }
        }

        return@withContext null
    }

    private fun buildTranscriptionPrompt(language: Language, vocabularyHints: List<String> = emptyList()): String {
        val langName = when (language) {
            Language.BENGALI -> "West Bengal Bengali (বাংলা)"
            Language.HINDI -> "Hindi (हिंदी)"
            Language.ENGLISH -> "Indian English"
        }
        val hintsSection = if (vocabularyHints.isNotEmpty()) {
            "\nVocabulary Hints (user's preferred spellings and local proper nouns): ${vocabularyHints.joinToString(", ")}"
        } else ""

        return "You are an elite speech-to-text transcriber for FlowKeys. Listen to this audio and transcribe exactly what was spoken in $langName. " +
                "The speaker may naturally use code-mixed terms, English loanwords, technical vocabulary, Indian names, or places. $hintsSection" +
                "\nCRITICAL: Output ONLY the verbatim spoken transcript in the appropriate native script (or Latin for English). Never include preamble, explanations, notes, or quotes."
    }

    private fun tryTranscribeWithModel(
        model: String,
        base64Audio: String,
        instruction: String,
        apiKey: String
    ): String? {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.doOutput = true

            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", instruction)
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "audio/wav")
                                    put("data", base64Audio)
                                })
                            })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 800)
                })
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                return parseGeminiResponse(responseText)
            } else {
                Log.w(TAG, "Gemini ASR ($model) returned HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.use { it.readText() }}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in Gemini ASR ($model)", e)
        }
        return null
    }

    /**
     * Translates or semantically polishes text adhering to the strict FlowKeys Quality Contract.
     * Supports direct Bengali <-> English, Bengali <-> Hindi, Hindi <-> English, and monolingual cleanup.
     */
    suspend fun translateOrPolish(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        apiKey: String,
        smartMode: SmartMode = SmartMode.GENERAL,
        vocabularyHints: List<String> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank() || apiKey.isBlank()) return@withContext text

        val prompt = buildLanguagePrompt(text, sourceLang, targetLang, smartMode, vocabularyHints)
        for (model in CANDIDATE_MODELS) {
            val result = tryGenerateText(model, prompt, apiKey)
            if (!result.isNullOrBlank()) {
                return@withContext result
            }
        }

        return@withContext null
    }

    private fun buildLanguagePrompt(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        smartMode: SmartMode,
        vocabularyHints: List<String> = emptyList()
    ): String {
        val isTranslation = sourceLang != targetLang
        val styleInstruction = when (smartMode) {
            SmartMode.CHAT, SmartMode.GENERAL -> "Format for conversational chat/messaging. Keep it natural, warm, and authentic."
            SmartMode.PROFESSIONAL -> "Format professionally for workplace/email communication with crisp, polished grammar."
            SmartMode.DEVELOPER -> "Format for developer environment, preserving code identifiers, CamelCase, snake_case, URLs, and technical syntax verbatim."
            SmartMode.RAW -> "Verbatim transcription without altering sentence structure."
        }

        val taskDesc = if (isTranslation) {
            "Translate the following input from ${sourceLang.displayName} directly into natural, contemporary ${targetLang.displayName}. Never translate via an intermediate language."
        } else {
            "Polish the following dictated ${sourceLang.displayName} text into natural written text."
        }

        val hintsRule = if (vocabularyHints.isNotEmpty()) {
            "\n8. Personal Vocabulary Hints: When resolving ambiguous terms, prefer these user vocabulary spellings: ${vocabularyHints.joinToString(", ")}"
        } else ""

        return """
You are the frontier language engine for FlowKeys.
$taskDesc
$styleInstruction

MANDATORY RULES:
1. Preserve meaning faithfully and exactly. Never invent facts, add details, or summarize.
2. West Bengal / Indian Conversational Awareness: Handle code-switching (Benglish, Hinglish) and technical loanwords (e.g. API, meeting, office, file, doctor, appointment, train, traffic, laptop) naturally without forced archaic substitutions.
3. Protect Proper Nouns: Keep people's names (e.g., Subhashish, Rahul, Rohan, Debolina, Ananya), cities/places (e.g., Kolkata, Durgapur, Asansol, Howrah, Siliguri), company names, apps (e.g., WhatsApp, Temenos, Upstox, Zerodha) intact.
4. Protect Spans: Retain numbers, dates, times, currencies (₹, $), URLs, emails, and code symbols verbatim.
5. Resolve Self-Corrections: If the speaker self-corrects (e.g., "Friday... actually Saturday" -> "Saturday"), output only the corrected intent.
6. Output Target Script: For Bengali use Bengali script (বাংলা), for Hindi use Devanagari script (हिंदी), for English use English alphabet.
7. CRITICAL: Output ONLY the final resulting sentence. Absolutely NO introductory text, NO quotes, NO explanation of edits.$hintsRule

Input Text:
$text
""".trimIndent()
    }

    private fun tryGenerateText(
        model: String,
        prompt: String,
        apiKey: String
    ): String? {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 7000
            conn.readTimeout = 10000
            conn.doOutput = true

            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 800)
                })
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                return parseGeminiResponse(responseText)
            } else {
                Log.w(TAG, "Gemini Text ($model) returned HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.use { it.readText() }}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in Gemini Text ($model)", e)
        }
        return null
    }

    private fun parseGeminiResponse(jsonString: String): String? {
        try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                sb.append(part.optString("text", ""))
            }
            val text = sb.toString().trim()
            // Clean any accidental markdown quotes or backticks emitted by the model
            return text.removePrefix("```").removeSuffix("```").trim().trim('"', '\'')
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response", e)
            return null
        }
    }
}
