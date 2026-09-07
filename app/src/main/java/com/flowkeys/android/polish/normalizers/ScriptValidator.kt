package com.flowkeys.android.polish.normalizers

import com.flowkeys.android.core.model.Language

/**
 * Enforces the script contract with Benglish/Hinglish awareness:
 * - English → Latin script (always passes)
 * - Hindi → Devanagari script (with English loanword tolerance)
 * - Bengali → Bengali script (with English loanword tolerance)
 *
 * Relaxed thresholds to support code-mixed speech (Benglish, Hinglish)
 * where speakers naturally mix English words into Indic sentences.
 */
object ScriptValidator {

    /**
     * Checks whether [text] conforms to the expected script for [language].
     * Allows code-mixed text (Benglish/Hinglish) by using a relaxed 15% threshold
     * and checking for presence of ANY target script characters.
     */
    fun isValidScript(text: String, language: Language): Boolean {
        if (text.isBlank()) return true

        val meaningfulChars = text.filter { !it.isWhitespace() && it !in ".,?!-–—:;\"'()[]{}/*+–।॥" }
        if (meaningfulChars.isEmpty()) return true

        val scriptCharCount = meaningfulChars.count { language.isScriptChar(it) }
        val scriptRatio = scriptCharCount.toDouble() / meaningfulChars.length

        return when (language) {
            // English always passes — Latin script covers digits, punctuation, etc.
            Language.ENGLISH -> true
            // For Hindi/Bengali: accept if ANY Indic characters present (Benglish/Hinglish support)
            // or if the ratio is above 15% (heavily code-mixed speech)
            Language.HINDI, Language.BENGALI -> {
                scriptRatio >= 0.15 || meaningfulChars.any { it in language.unicodeRangeStart..language.unicodeRangeEnd }
            }
        }
    }

    /**
     * Detects if text is code-mixed (contains characters from multiple scripts).
     */
    fun isCodeMixed(text: String): Boolean {
        if (text.isBlank()) return false
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        val scriptCount = listOf(hasLatin, hasDevanagari, hasBengali).count { it }
        return scriptCount >= 2
    }
}

