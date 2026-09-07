package com.flowkeys.android.core.model

/**
 * First-class supported languages in FlowKeys Android.
 * Each language defines its ISO code, localized display label, native script name,
 * and Unicode range for strict script validation.
 */
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val unicodeRangeStart: Char,
    val unicodeRangeEnd: Char
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        unicodeRangeStart = '\u0020',
        unicodeRangeEnd = '\u007F'
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिंदी",
        unicodeRangeStart = '\u0900',
        unicodeRangeEnd = '\u097F'
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali (West Bengal)",
        nativeName = "বাংলা",
        unicodeRangeStart = '\u0980',
        unicodeRangeEnd = '\u09FF'
    );

    val speechLocaleTag: String
        get() = when (this) {
            ENGLISH -> "en-IN"
            HINDI -> "hi-IN"
            BENGALI -> "bn-IN"
        }

    /**
     * Checks if a character belongs to the primary script of this language.
     */
    fun isScriptChar(c: Char): Boolean {
        return when (this) {
            ENGLISH -> c.isLetterOrDigit() || c.isWhitespace() || "!@#$%^&*()_+-=[]{}|;':\",./<>?~`".contains(c)
            HINDI -> c in unicodeRangeStart..unicodeRangeEnd || c == '।' || c == '॥' || c.isWhitespace()
            BENGALI -> c in unicodeRangeStart..unicodeRangeEnd || c == '।' || c == '॥' || c.isWhitespace()
        }
    }

    companion object {
        val DEFAULT = ENGLISH

        fun fromCode(code: String): Language {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
        }

        /**
         * Detects the dominant language of [text] based on Unicode script analysis.
         * Useful for auto-detecting language of code-mixed input.
         */
        fun detect(text: String): Language {
            if (text.isBlank()) return DEFAULT
            val bengaliCount = text.count { it in '\u0980'..'\u09FF' }
            val devanagariCount = text.count { it in '\u0900'..'\u097F' }
            val latinCount = text.count { it in 'A'..'Z' || it in 'a'..'z' }
            return when {
                bengaliCount > devanagariCount && bengaliCount > latinCount -> BENGALI
                devanagariCount > bengaliCount && devanagariCount > latinCount -> HINDI
                else -> ENGLISH
            }
        }
    }

    /**
     * Whether this language commonly code-mixes with [other].
     * Bengali+English = Benglish, Hindi+English = Hinglish.
     */
    fun isCodeMixedWith(other: Language): Boolean {
        return (this == BENGALI && other == ENGLISH) ||
               (this == ENGLISH && other == BENGALI) ||
               (this == HINDI && other == ENGLISH) ||
               (this == ENGLISH && other == HINDI)
    }

    /**
     * Locale tag for code-mixed speech recognition.
     * Uses the primary language tag — Android's SpeechRecognizer handles
     * English loanwords within Indic speech automatically.
     */
    val codeMixLocaleTag: String
        get() = speechLocaleTag
}
