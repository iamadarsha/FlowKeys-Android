package com.flowkeys.android.core.model

/**
 * Output script rendering mode for speech recognition in FlowKeys:
 * - TRANSLATED_ENGLISH: Translates Indic speech to English (e.g. "I will come tomorrow.")
 * - NATIVE_SCRIPT: Output in native Indic script (e.g. "আমি কাল আসব।" / "मैं कल आऊँगा।")
 * - PHONETIC_LATIN: Output in natural phonetic Latin script (Benglish/Hinglish) (e.g. "Ami kal ashbo." / "Main kal aaunga.")
 */
enum class ScriptMode(val id: String, val displayName: String, val badgeLabel: String) {
    TRANSLATED_ENGLISH("translated_en", "Translate to English", "→ EN"),
    NATIVE_SCRIPT("native_script", "Native Script", "Original"),
    PHONETIC_LATIN("phonetic_latin", "Phonetic Latin (Benglish/Hinglish)", "Latin");

    companion object {
        val DEFAULT = TRANSLATED_ENGLISH

        fun fromId(id: String?): ScriptMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
