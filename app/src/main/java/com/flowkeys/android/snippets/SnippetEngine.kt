package com.flowkeys.android.snippets

/**
 * Deterministic text macro and snippet expansion engine.
 * Expands phrases like "my signature", "my email", "my address" instantly before insertion.
 */
object SnippetEngine {

    private val snippets = mutableMapOf(
        "my email" to "user@flowkeys.ai",
        "my signature" to "Best regards,\nAdarsha",
        "my website" to "https://flowkeys.ai"
    )

    fun expand(text: String): String {
        var result = text
        for ((trigger, expansion) in snippets) {
            result = result.replace(Regex("(?i)\\b${Regex.escape(trigger)}\\b"), expansion)
        }
        return result
    }

    fun registerSnippet(trigger: String, expansion: String) {
        snippets[trigger.trim().lowercase()] = expansion.trim()
    }
}
