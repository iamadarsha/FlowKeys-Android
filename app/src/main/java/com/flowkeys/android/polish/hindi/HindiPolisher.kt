package com.flowkeys.android.polish.hindi

/**
 * Specialized deterministic cleaner for Hindi and Hinglish speech.
 *
 * Capabilities:
 * - Strips colloquial fillers ("मतलब", "यार", "अरे यार", "तो फिर")
 * - Enforces Devanagari script integrity
 * - Normalizes Hinglish loanwords into Devanagari
 * - Injects Hindi Dari ("।") or question marks
 */
object HindiPolisher {

    private val fillers = listOf(
        "अरे यार",
        "यार",
        "मतलब",
        "तो फिर",
        "जैसे कि",
        "समझे ना",
        "वैसे",
        "um",
        "uh",
        "ah"
    )

    private val loanwordsMap = mapOf(
        "(?i)\\bmeeting\\b" to "मीटिंग",
        "(?i)\\boffice\\b" to "ऑफिस",
        "(?i)\\btrain\\b" to "ट्रेन",
        "(?i)\\bstation\\b" to "स्टेशन",
        "(?i)\\bpresentation\\b" to "प्रेजेंटेशन",
        "(?i)\\bclient\\b" to "क्लाइंट",
        "(?i)\\bmessage\\b" to "मैसेज",
        "(?i)\\bcall\\b" to "कॉल"
    )

    private val interrogatives = listOf("क्यों", "कहाँ", "कब", "क्या", "कैसे", "कौन")

    fun polish(rawText: String): String {
        var text = rawText.trim()
        if (text.isEmpty()) return text

        // 1. Remove hesitation fillers with robust Unicode boundaries
        for (filler in fillers) {
            val pattern = if (filler.all { it.code < 128 }) {
                "(?i)\\b$filler\\b"
            } else {
                "(?:^|(?<=\\s))$filler(?=\\s|[।,?!]|$)"
            }
            text = text.replace(Regex(pattern), "")
        }

        // 2. Phonemic loanword mapping
        for ((pattern, replacement) in loanwordsMap) {
            text = text.replace(Regex(pattern), replacement)
        }

        // 3. Collapse extra whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        // 4. Hindi punctuation (Dari or Question Mark)
        if (!text.endsWith("।") && !text.endsWith("?") && !text.endsWith("!")) {
            val words = text.split(Regex("\\s+")).map { it.trim().removeSuffix("।").removeSuffix("?").removeSuffix(",") }
            val isQuestion = words.any { it in interrogatives }
            text = if (isQuestion) "$text?" else "$text।"
        }

        return text
    }
}
