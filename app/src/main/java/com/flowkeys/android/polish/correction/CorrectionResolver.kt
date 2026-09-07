package com.flowkeys.android.polish.correction

import com.flowkeys.android.core.model.Language

/**
 * Detects and resolves conversational self-corrections, backtracks, and stutters.
 */
object CorrectionResolver {

    private val englishCorrectionTriggers = listOf("no wait", "sorry", "actually", "rather", "i mean")
    private val hindiCorrectionTriggers = listOf("नहीं नहीं", "नहीं", "रुको", "मतलब मेरा मतलब")
    private val bengaliCorrectionTriggers = listOf("না না", "না", "দাঁড়াও", "ভুল বললাম", "মানে বলছি")

    fun resolve(text: String, language: Language): String {
        var cleaned = collapseStutters(text)

        val triggers = when (language) {
            Language.ENGLISH -> englishCorrectionTriggers
            Language.HINDI -> hindiCorrectionTriggers
            Language.BENGALI -> bengaliCorrectionTriggers
        }

        for (trigger in triggers) {
            val regex = Regex("(?i)\\b${Regex.escape(trigger)}\\b")
            val match = regex.find(cleaned)
            if (match != null) {
                val postClause = cleaned.substring(match.range.last + 1).trim()
                if (postClause.length > 2) {
                    // If there is substantial content after the correction trigger, prefer the correction
                    cleaned = postClause
                    break
                }
            }
        }

        return cleaned
    }

    /**
     * Collapses repeated adjacent words ("I I" -> "I", "আমি আমি" -> "আমি", "कल कल" -> "कल").
     */
    private fun collapseStutters(text: String): String {
        return text.replace(Regex("(?i)\\b(\\p{L}+)\\s+\\1\\b"), "$1")
    }
}
