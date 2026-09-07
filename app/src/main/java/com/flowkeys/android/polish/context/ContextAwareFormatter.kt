package com.flowkeys.android.polish.context

import com.flowkeys.android.core.model.DictationContext
import com.flowkeys.android.core.model.FieldType
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.modes.SmartMode

/**
 * Context-aware intelligent text formatter.
 * Adapts output formatting dynamically based on whether user is typing inside:
 * - Search bar (Chrome, YouTube) -> removes trailing period/dari
 * - Developer terminal (Termux, GitHub) -> keeps exact symbols & casing
 * - Messaging / Chat (WhatsApp, Telegram) -> natural chat formatting
 * - Email / Professional (Gmail, Outlook) -> auto-capitalizes greetings & bullet points
 */
object ContextAwareFormatter {

    fun format(
        text: String,
        language: Language,
        context: DictationContext?,
        smartMode: SmartMode = SmartMode.GENERAL
    ): String {
        if (text.isBlank()) return text
        var result = text.trim()

        val effectiveFieldType = context?.fieldType ?: when (smartMode) {
            SmartMode.DEVELOPER, SmartMode.RAW -> FieldType.DEVELOPER_TERMINAL
            SmartMode.PROFESSIONAL -> FieldType.EMAIL
            SmartMode.CHAT -> FieldType.MESSAGING
            SmartMode.GENERAL -> FieldType.GENERIC_TEXT
        }

        when (effectiveFieldType) {
            FieldType.SEARCH_BAR -> {
                // Remove trailing punctuation for search queries
                result = result.removeSuffix(".").removeSuffix("।").removeSuffix("?").removeSuffix("!").trim()
            }
            FieldType.DEVELOPER_TERMINAL -> {
                // Keep raw technical content without added sentence casing
                return result
            }
            FieldType.EMAIL -> {
                // Ensure salutations and sign-offs are formatted nicely
                result = formatEmailStructure(result, language)
            }
            FieldType.MESSAGING -> {
                // Casual conversational messaging heuristics
                result = formatMessagingStructure(result)
            }
            else -> {
                // Standard text
            }
        }

        return result
    }

    private fun formatEmailStructure(text: String, language: Language): String {
        var res = text
        // Capitalize common email greeting starters
        val greetings = listOf("hi", "hello", "dear", "good morning", "good evening", "hey")
        for (g in greetings) {
            val pattern = "(?i)^($g\\b\\s*[A-Za-z0-9_]*)([,!])?"
            res = res.replace(Regex(pattern)) { m ->
                val greetingPart = m.value
                val cap = greetingPart.replaceFirstChar { it.uppercase() }
                if (!cap.endsWith(",") && !cap.endsWith("!")) "$cap," else cap
            }
        }
        return res
    }

    private fun formatMessagingStructure(text: String): String {
        // In chat, single word replies ("ok", "sure", "done", "yes") don't need heavy punctuation
        val words = text.split(Regex("\\s+"))
        if (words.size <= 2) {
            return text.removeSuffix(".").removeSuffix("।")
        }
        return text
    }
}
