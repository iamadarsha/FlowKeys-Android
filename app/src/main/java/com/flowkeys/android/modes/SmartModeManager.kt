package com.flowkeys.android.modes

/**
 * Smart formatting modes for FlowKeys dictation.
 */
enum class SmartMode(val displayName: String, val description: String) {
    GENERAL("General", "Balanced cleanup and natural punctuation."),
    PROFESSIONAL("Professional", "Polished corporate communications and formal grammar."),
    CHAT("Chat / Casual", "Concise, expressive, conversational formatting."),
    DEVELOPER("Developer", "Literal script, preserves technical identifiers, code tokens, and URLs."),
    RAW("Raw / Literal", "Direct verbatim output with zero formatting or semantic alterations.")
}

/**
 * Automatically suggests a SmartMode based on the hosting application package.
 */
object AppAwareDetector {

    fun detectMode(packageName: String): SmartMode {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("termux") || pkg.contains("github") || pkg.contains("code") -> SmartMode.DEVELOPER
            pkg.contains("gm") || pkg.contains("mail") || pkg.contains("outlook") || pkg.contains("linkedin") -> SmartMode.PROFESSIONAL
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") || pkg.contains("messaging") -> SmartMode.CHAT
            else -> SmartMode.GENERAL
        }
    }
}
