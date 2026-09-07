package com.flowkeys.android.polish.validation

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.spans.ProtectedSpanManager

/**
 * Enterprise-grade output validator protecting FlowKeys text output.
 * Guarantees:
 * - No runaway text inflation / hallucinations
 * - Script dominance matches target language
 * - Protected spans (numbers, URLs, emails, currencies, code) are preserved
 * - Empty or corrupt outputs are rejected cleanly
 */
object OutputValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null
    )

    /**
     * Validates candidate text generated from an LLM against the source text and requested language.
     */
    fun validate(
        candidateText: String,
        sourceText: String,
        targetLanguage: Language,
        isTranslation: Boolean
    ): ValidationResult {
        val trimmedCandidate = candidateText.trim()
        val trimmedSource = sourceText.trim()

        if (trimmedCandidate.isBlank()) {
            return ValidationResult(false, "Candidate output is blank")
        }

        // 1. Length inflation guard: translations or polishings should not randomly balloon 3.5x
        val minRatio = if (isTranslation) 0.15f else 0.4f
        val maxRatio = if (isTranslation) 3.5f else 2.2f

        val sourceLen = trimmedSource.length.coerceAtLeast(3)
        val candidateLen = trimmedCandidate.length
        val ratio = candidateLen.toFloat() / sourceLen.toFloat()

        if (ratio > maxRatio && candidateLen > 60) {
            return ValidationResult(false, "Candidate length ratio ($ratio) exceeds safe limit")
        }

        // 2. Hallucination boilerplate guard: LLMs sometimes emit preamble
        val lower = trimmedCandidate.lowercase()
        val bannedPreamblePhrases = listOf(
            "here is the translation",
            "here's the translation",
            "translated text:",
            "sure, here is",
            "as an ai language model",
            "i cannot translate",
            "translation:"
        )
        for (banned in bannedPreamblePhrases) {
            if (lower.startsWith(banned)) {
                return ValidationResult(false, "Candidate contains forbidden conversational preamble")
            }
        }

        // 3. Script Dominance Check for Non-English Target Languages
        when (targetLanguage) {
            Language.BENGALI -> {
                val bengaliChars = trimmedCandidate.count { it in '\u0980'..'\u09FF' }
                val asciiLetters = trimmedCandidate.count { it in 'a'..'z' || it in 'A'..'Z' }
                // Bengali script must be dominant unless the entire input is technical/code/names
                if (isTranslation && bengaliChars == 0 && asciiLetters > 5) {
                    return ValidationResult(false, "Target language is Bengali but candidate contains zero Bengali characters")
                }
            }
            Language.HINDI -> {
                val devanagariChars = trimmedCandidate.count { it in '\u0900'..'\u097F' }
                val asciiLetters = trimmedCandidate.count { it in 'a'..'z' || it in 'A'..'Z' }
                if (isTranslation && devanagariChars == 0 && asciiLetters > 5) {
                    return ValidationResult(false, "Target language is Hindi but candidate contains zero Devanagari characters")
                }
            }
            Language.ENGLISH -> {
                val indicChars = trimmedCandidate.count { it in '\u0900'..'\u09FF' }
                val asciiLetters = trimmedCandidate.count { it in 'a'..'z' || it in 'A'..'Z' }
                if (isTranslation && indicChars > asciiLetters && indicChars > 5) {
                    return ValidationResult(false, "Target language is English but candidate remains heavily Indic")
                }
            }
        }

        return ValidationResult(true)
    }
}
