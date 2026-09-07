package com.flowkeys.android.polish.english

import com.flowkeys.android.polish.translation.OfflineIndicLexicon
import java.util.Locale

/**
 * Deterministic cleaner for English speech.
 *
 * Capabilities:
 * - Strips fillers ("um", "uh", "you know", "basically", "actually", "like")
 * - Capitalizes sentence beginnings
 * - Terminal punctuation heuristics
 */
object EnglishPolisher {

    private val fillers = listOf(
        "(?i)\\bumm?\\b",
        "(?i)\\buhh?\\b",
        "(?i)\\berm?\\b",
        "(?i)\\byou know\\b",
        "(?i)\\bbasically\\b",
        "(?i)\\bactually\\b",
        "(?i)\\bsort of\\b",
        "(?i)\\bkind of\\b"
    )

    private val questionStarters = listOf("what", "when", "where", "who", "why", "how", "can", "could", "would", "is", "are", "do", "does")

    fun polish(rawText: String): String {
        var text = rawText.trim()
        if (text.isEmpty()) return text

        // 1. Strip fillers
        for (filler in fillers) {
            text = text.replace(Regex(filler), "")
        }

        // 2. Expand Indian English idioms and WhatsApp slang
        text = OfflineIndicLexicon.expandInternetSlang(text)

        // 3. Collapse extra whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        // 3. Sentence capitalization
        text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        // 4. Terminal punctuation
        if (!text.endsWith(".") && !text.endsWith("?") && !text.endsWith("!")) {
            val firstWord = text.split(" ").firstOrNull()?.lowercase() ?: ""
            val isQuestion = questionStarters.contains(firstWord)
            text = if (isQuestion) "$text?" else "$text."
        }

        return text
    }
}
