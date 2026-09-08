package com.flowkeys.android.stats

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
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 100% Local, Offline, Privacy-Preserving Usage Statistics Engine for FlowKeys.
 *
 * Tracks:
 * - Total words spoken, dictation sessions, and characters generated
 * - Daily streaks (current and longest)
 * - Language distribution (English, Hindi, Bengali)
 * - Estimated time saved (clearly labeled calculation based on standard 35 wpm typing vs 140 wpm speaking)
 *
 * Zero analytics SDKs. Zero network telemetry.
 */
data class LocalStats(
    val totalWordsSpoken: Long = 0L,
    val totalSessions: Long = 0L,
    val totalCharacters: Long = 0L,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastSessionDate: String = "", // "yyyy-MM-dd"
    val englishWords: Long = 0L,
    val hindiWords: Long = 0L,
    val bengaliWords: Long = 0L,
    val englishSessions: Long = 0L,
    val hindiSessions: Long = 0L,
    val bengaliSessions: Long = 0L
) {
    /**
     * Estimated time saved in minutes:
     * (words / 35.0) - (words / 140.0)
     */
    val estimatedMinutesSaved: Double
        get() {
            if (totalWordsSpoken <= 0) return 0.0
            val typingMinutes = totalWordsSpoken / 35.0
            val speakingMinutes = totalWordsSpoken / 140.0
            return (typingMinutes - speakingMinutes).coerceAtLeast(0.0)
        }

    val formattedTimeSaved: String
        get() {
            val totalMinutes = estimatedMinutesSaved.toInt()
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }
        }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("totalWordsSpoken", totalWordsSpoken)
        put("totalSessions", totalSessions)
        put("totalCharacters", totalCharacters)
        put("currentStreakDays", currentStreakDays)
        put("longestStreakDays", longestStreakDays)
        put("lastSessionDate", lastSessionDate)
        put("englishWords", englishWords)
        put("hindiWords", hindiWords)
        put("bengaliWords", bengaliWords)
        put("englishSessions", englishSessions)
        put("hindiSessions", hindiSessions)
        put("bengaliSessions", bengaliSessions)
    }

    companion object Serializer {
        fun fromJsonObject(json: JSONObject): LocalStats {
            return LocalStats(
                totalWordsSpoken = json.optLong("totalWordsSpoken", 0L),
                totalSessions = json.optLong("totalSessions", 0L),
                totalCharacters = json.optLong("totalCharacters", 0L),
                currentStreakDays = json.optInt("currentStreakDays", 0),
                longestStreakDays = json.optInt("longestStreakDays", 0),
                lastSessionDate = json.optString("lastSessionDate", ""),
                englishWords = json.optLong("englishWords", 0L),
                hindiWords = json.optLong("hindiWords", 0L),
                bengaliWords = json.optLong("bengaliWords", 0L),
                englishSessions = json.optLong("englishSessions", 0L),
                hindiSessions = json.optLong("hindiSessions", 0L),
                bengaliSessions = json.optLong("bengaliSessions", 0L)
            )
        }
    }
}

object LocalStatsRepository {

    private const val TAG = "LocalStatsRepository"
    private const val FILE_NAME = "local_stats.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var statsFile: File? = null

    private val _stats = MutableStateFlow(LocalStats())
    val stats: StateFlow<LocalStats> = _stats.asStateFlow()

    fun initialize(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        statsFile = file
        loadFromFile(file)
    }

    fun initializeForTesting(file: File) {
        statsFile = file
        _stats.value = LocalStats()
        loadFromFile(file)
    }

    private fun loadFromFile(file: File) {
        try {
            if (!file.exists()) return
            val content = file.readText()
            if (content.isNotBlank()) {
                val json = JSONObject(content)
                _stats.value = LocalStats.fromJsonObject(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local stats", e)
        }
    }

    private fun persistToFile() {
        val file = statsFile ?: return
        try {
            val json = _stats.value.toJsonObject()
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(json.toString())
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local stats", e)
        }
    }

    /**
     * Records a completed dictation session.
     */
    fun recordSession(text: String, language: Language, packageName: String? = null) {
        if (text.isBlank()) return

        scope.launch {
            val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val wordCount = words.size.toLong()
            val charCount = text.length.toLong()

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val current = _stats.value

            // Streak logic
            val (newCurrentStreak, newLongestStreak) = calculateStreak(current.lastSessionDate, today, current.currentStreakDays, current.longestStreakDays)

            val updated = current.copy(
                totalWordsSpoken = current.totalWordsSpoken + wordCount,
                totalSessions = current.totalSessions + 1L,
                totalCharacters = current.totalCharacters + charCount,
                currentStreakDays = newCurrentStreak,
                longestStreakDays = newLongestStreak,
                lastSessionDate = today,
                englishWords = if (language == Language.ENGLISH) current.englishWords + wordCount else current.englishWords,
                hindiWords = if (language == Language.HINDI) current.hindiWords + wordCount else current.hindiWords,
                bengaliWords = if (language == Language.BENGALI) current.bengaliWords + wordCount else current.bengaliWords,
                englishSessions = if (language == Language.ENGLISH) current.englishSessions + 1L else current.englishSessions,
                hindiSessions = if (language == Language.HINDI) current.hindiSessions + 1L else current.hindiSessions,
                bengaliSessions = if (language == Language.BENGALI) current.bengaliSessions + 1L else current.bengaliSessions
            )

            _stats.value = updated
            persistToFile()
        }
    }

    private fun calculateStreak(lastDateStr: String, todayStr: String, currentStreak: Int, longestStreak: Int): Pair<Int, Int> {
        if (lastDateStr.isBlank()) {
            return 1 to 1.coerceAtLeast(longestStreak)
        }
        if (lastDateStr == todayStr) {
            // Already counted for today
            return currentStreak to longestStreak
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val lastDate = sdf.parse(lastDateStr) ?: return 1 to 1.coerceAtLeast(longestStreak)
            val cal = Calendar.getInstance().apply {
                time = lastDate
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val yesterdayFormatted = sdf.format(cal.time)

            val newStreak = if (yesterdayFormatted == todayStr) {
                currentStreak + 1
            } else {
                1
            }
            val newLongest = newStreak.coerceAtLeast(longestStreak)
            return newStreak to newLongest
        } catch (e: Exception) {
            return 1 to 1.coerceAtLeast(longestStreak)
        }
    }

    fun clearStats() {
        scope.launch {
            _stats.value = LocalStats()
            persistToFile()
        }
    }
}
