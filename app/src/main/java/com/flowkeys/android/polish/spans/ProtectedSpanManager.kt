package com.flowkeys.android.polish.spans

import java.util.regex.Pattern

/**
 * Identifies and isolates immutable text spans (URLs, emails, phone numbers,
 * currency figures, code identifiers, hashtags, mentions) before text polishing,
 * ensuring zero mutation or linguistic corruption of technical or structured data.
 */
object ProtectedSpanManager {

    data class MaskResult(
        val maskedText: String,
        val placeholderMap: Map<String, String>
    )

    private val URL_PATTERN = Pattern.compile(
        "\\b(?:https?://|ftp://|www\\.)[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+|\\b[a-zA-Z0-9.-]+\\.(?:com|org|net|in|io|ai|co|app|dev)(?:/[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val EMAIL_PATTERN = Pattern.compile(
        "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val PHONE_PATTERN = Pattern.compile(
        "(?:\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}\\b|\\b\\+?\\d{1,3}[\\s-]?\\d{3,4}[\\s-]?\\d{4}\\b"
    )

    private val CURRENCY_PATTERN = Pattern.compile(
        "(?:[₹$€£¥]\\s*\\d+(?:,\\d+)*(?:\\.\\d+)?)|(?:\\b\\d+(?:,\\d+)*(?:\\.\\d+)?\\s*(?:rupees|rs\\.?|dollars?|inr|usd)\\b)",
        Pattern.CASE_INSENSITIVE
    )

    private val CODE_IDENTIFIER_PATTERN = Pattern.compile(
        "\\b[a-z]+(?:[A-Z][a-z0-9]+)+\\b|\\b[a-z0-9]+(?:_[a-z0-9]+)+\\b"
    )

    private val SOCIAL_TAG_PATTERN = Pattern.compile(
        "[@#][a-zA-Z0-9_]+"
    )

    /**
     * Extracts and masks protected spans with deterministic placeholders.
     */
    fun mask(text: String): MaskResult {
        if (text.isBlank()) return MaskResult(text, emptyMap())

        val placeholders = mutableMapOf<String, String>()
        var counter = 0
        var currentText = text

        // Order matters: emails and URLs before code/phone patterns
        val patterns = listOf(
            EMAIL_PATTERN,
            URL_PATTERN,
            CURRENCY_PATTERN,
            PHONE_PATTERN,
            CODE_IDENTIFIER_PATTERN,
            SOCIAL_TAG_PATTERN
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(currentText)
            val sb = StringBuffer()
            while (matcher.find()) {
                val span = matcher.group()
                // Avoid re-masking existing placeholders
                if (span.startsWith("__FK_SPAN_")) continue

                val placeholder = "__FK_SPAN_${counter++}__"
                placeholders[placeholder] = span
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(placeholder))
            }
            matcher.appendTail(sb)
            currentText = sb.toString()
        }

        return MaskResult(currentText, placeholders)
    }

    /**
     * Restores protected spans from their placeholders back into the polished text.
     */
    fun unmask(text: String, placeholders: Map<String, String>): String {
        if (text.isBlank() || placeholders.isEmpty()) return text

        var result = text
        for ((placeholder, originalValue) in placeholders) {
            result = result.replace(placeholder, originalValue)
        }
        return result
    }
}
