package com.flowkeys.android.dictionary

import android.content.Context
import android.util.Log
import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Layer B: Local Privacy-Preserving Learned Vocabulary Store.
 *
 * 100% on-device learning:
 * - Frequently spoken terms and proper nouns
 * - User correction mappings (e.g. "Rhaul" -> "Rahul")
 * - Generates compact Top-N context hints for Gemini prompts
 * - Fully reversible: users can view, delete, clear, or disable learning.
 * - Zero network leakage; never requires Contacts permission.
 */
data class LearnedVocabularyEntry(
    val term: String,
    val canonicalForm: String,
    val languageCode: String,
    val frequency: Int = 1,
    val correctionCount: Int = 0,
    val firstSeenMs: Long = System.currentTimeMillis(),
    val lastSeenMs: Long = System.currentTimeMillis(),
    val confidence: Float = 0.5f,
    val source: String = "FREQUENCY" // "FREQUENCY", "CORRECTION", "PROPER_NOUN", "MANUAL"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("term", term)
        put("canonicalForm", canonicalForm)
        put("languageCode", languageCode)
        put("frequency", frequency)
        put("correctionCount", correctionCount)
        put("firstSeenMs", firstSeenMs)
        put("lastSeenMs", lastSeenMs)
        put("confidence", confidence.toDouble())
        put("source", source)
    }

    companion object {
        const val SOURCE_FREQUENCY = "FREQUENCY"
        const val SOURCE_CORRECTION = "CORRECTION"
        const val SOURCE_PROPER_NOUN = "PROPER_NOUN"
        const val SOURCE_MANUAL = "MANUAL"

        fun fromJsonObject(json: JSONObject): LearnedVocabularyEntry {
            return LearnedVocabularyEntry(
                term = json.optString("term"),
                canonicalForm = json.optString("canonicalForm", json.optString("term")),
                languageCode = json.optString("languageCode", "en"),
                frequency = json.optInt("frequency", 1),
                correctionCount = json.optInt("correctionCount", 0),
                firstSeenMs = json.optLong("firstSeenMs", System.currentTimeMillis()),
                lastSeenMs = json.optLong("lastSeenMs", System.currentTimeMillis()),
                confidence = json.optDouble("confidence", 0.5).toFloat(),
                source = json.optString("source", SOURCE_FREQUENCY)
            )
        }
    }
}

object LearnedVocabularyStore {

    private const val TAG = "LearnedVocabularyStore"
    private const val FILE_NAME = "learned_vocabulary.json"
    private const val VERSION = 1

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var storageFile: File? = null
    private var isEnabled = true

    // Keyed by lowercase term
    private val entries = ConcurrentHashMap<String, LearnedVocabularyEntry>()

    private val _vocabularyList = MutableStateFlow<List<LearnedVocabularyEntry>>(emptyList())
    val vocabularyList: StateFlow<List<LearnedVocabularyEntry>> = _vocabularyList.asStateFlow()

    fun initialize(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        storageFile = file
        loadFromFile(file)
    }

    /**
     * Initializes with an explicit file for testability.
     */
    fun initializeForTesting(file: File) {
        storageFile = file
        entries.clear()
        isEnabled = true
        loadFromFile(file)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isLearningEnabled(): Boolean = isEnabled

    private fun loadFromFile(file: File) {
        try {
            if (!file.exists()) {
                _vocabularyList.value = emptyList()
                return
            }
            val content = file.readText()
            if (content.isBlank()) return

            val root = JSONObject(content)
            val jsonArray = root.optJSONArray("entries") ?: JSONArray()
            val loaded = mutableListOf<LearnedVocabularyEntry>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val entry = LearnedVocabularyEntry.fromJsonObject(obj)
                if (entry.term.isNotBlank()) {
                    entries[entry.term.lowercase()] = entry
                    loaded.add(entry)
                }
            }
            _vocabularyList.value = loaded.sortedByDescending { it.frequency }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading learned vocabulary", e)
        }
    }

    private fun persistToFile() {
        val file = storageFile ?: return
        try {
            val root = JSONObject().apply {
                put("version", VERSION)
                put("timestamp", System.currentTimeMillis())
                val array = JSONArray()
                entries.values.forEach { array.put(it.toJsonObject()) }
                put("entries", array)
            }
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(root.toString())
            if (tempFile.renameTo(file)) {
                // Success
            } else {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
            _vocabularyList.value = entries.values.sortedByDescending { it.frequency }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving learned vocabulary", e)
        }
    }

    /**
     * Records word occurrences from a completed dictation.
     * Extracts multi-letter words and proper nouns without saving full sentence transcripts.
     */
    fun recordDictationWords(text: String, language: Language, packageName: String? = null) {
        if (!isEnabled || text.isBlank()) return

        scope.launch {
            // Tokenize by whitespace and punctuation, keeping words with length >= 3
            val tokens = text.split(Regex("[\\s\\p{Punct}&&[^@_]]+"))
                .filter { it.length >= 3 && !it.all { char -> char.isDigit() } }

            val now = System.currentTimeMillis()
            var mutated = false

            for (token in tokens) {
                // Skip common stop words in English/Latin script
                if (isCommonStopWord(token)) continue

                val lower = token.lowercase()
                val isCapitalized = token.first().isUpperCase() && token.drop(1).any { it.isLowerCase() }
                val existing = entries[lower]

                if (existing != null) {
                    val newFreq = existing.frequency + 1
                    val newCanonical = if (isCapitalized && existing.canonicalForm.all { it.isLowerCase() }) token else existing.canonicalForm
                    val newConfidence = (existing.confidence + 0.1f).coerceAtMost(1.0f)
                    entries[lower] = existing.copy(
                        canonicalForm = newCanonical,
                        frequency = newFreq,
                        lastSeenMs = now,
                        confidence = newConfidence
                    )
                    mutated = true
                } else if (isCapitalized || token.length >= 5) {
                    // Introduce candidate proper noun or longer term
                    val source = if (isCapitalized) LearnedVocabularyEntry.SOURCE_PROPER_NOUN else LearnedVocabularyEntry.SOURCE_FREQUENCY
                    entries[lower] = LearnedVocabularyEntry(
                        term = token,
                        canonicalForm = token,
                        languageCode = language.code,
                        frequency = 1,
                        firstSeenMs = now,
                        lastSeenMs = now,
                        confidence = if (isCapitalized) 0.6f else 0.4f,
                        source = source
                    )
                    mutated = true
                }
            }

            if (mutated) {
                persistToFile()
            }
        }
    }

    /**
     * Records an explicit or inferred user correction (e.g. user replaced "Rhaul" with "Rahul").
     * This provides a very high confidence learning signal.
     */
    fun recordCorrection(spokenTerm: String, correctedTerm: String, language: Language) {
        if (!isEnabled || spokenTerm.isBlank() || correctedTerm.isBlank()) return

        scope.launch {
            val lower = spokenTerm.trim().lowercase()
            val canonical = correctedTerm.trim()
            val now = System.currentTimeMillis()
            val existing = entries[lower]

            val count = (existing?.correctionCount ?: 0) + 1
            val freq = (existing?.frequency ?: 1) + 1

            entries[lower] = LearnedVocabularyEntry(
                term = spokenTerm.trim(),
                canonicalForm = canonical,
                languageCode = language.code,
                frequency = freq,
                correctionCount = count,
                firstSeenMs = existing?.firstSeenMs ?: now,
                lastSeenMs = now,
                confidence = 0.95f,
                source = LearnedVocabularyEntry.SOURCE_CORRECTION
            )

            // Also register in PersonalDictionary for immediate rule application
            PersonalDictionary.addWord(spokenTerm.trim(), canonical)
            persistToFile()
        }
    }

    /**
     * Returns top-ranked vocabulary candidates to inject as context hints into Gemini prompt.
     * Compact (max limit items) to preserve prompt token economy and strict privacy.
     */
    fun getTopHints(language: Language, limit: Int = 12): List<String> {
        if (!isEnabled || entries.isEmpty()) return emptyList()

        return entries.values
            .filter { it.languageCode == language.code || it.source == LearnedVocabularyEntry.SOURCE_CORRECTION }
            .sortedWith(
                compareByDescending<LearnedVocabularyEntry> { it.source == LearnedVocabularyEntry.SOURCE_CORRECTION }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.frequency }
            )
            .take(limit)
            .map { it.canonicalForm }
            .distinct()
    }

    /**
     * Deletes a specific learned vocabulary entry by term.
     */
    fun deleteEntry(term: String) {
        val removed = entries.remove(term.lowercase())
        if (removed != null) {
            scope.launch {
                persistToFile()
            }
        }
    }

    /**
     * Clears all learned vocabulary while preserving manual dictionary entries.
     */
    fun clearAll() {
        entries.clear()
        scope.launch {
            persistToFile()
        }
    }

    fun getEntryCount(): Int = entries.size

    fun getCorrectionCount(): Int = entries.values.count { it.correctionCount > 0 }

    private fun isCommonStopWord(word: String): Boolean {
        val w = word.lowercase()
        return w in setOf(
            "the", "and", "that", "have", "for", "not", "with", "you", "this", "but",
            "his", "from", "they", "say", "her", "she", "will", "one", "all", "would",
            "there", "their", "what", "out", "about", "who", "get", "which", "when",
            "make", "can", "like", "time", "just", "him", "know", "take", "people",
            "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think",
            "also", "back", "after", "use", "two", "how", "our", "work", "first",
            "well", "way", "even", "new", "want", "because", "any", "these", "give"
        )
    }
}
