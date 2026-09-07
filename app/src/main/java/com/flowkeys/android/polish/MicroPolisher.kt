package com.flowkeys.android.polish

import com.flowkeys.android.core.model.DictationContext
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.dictionary.PersonalDictionary
import com.flowkeys.android.modes.SmartMode
import com.flowkeys.android.polish.bengali.BengaliPolisher
import com.flowkeys.android.polish.commands.VoiceCommandEngine
import com.flowkeys.android.polish.context.ContextAwareFormatter
import com.flowkeys.android.polish.correction.CorrectionResolver
import com.flowkeys.android.polish.english.EnglishPolisher
import com.flowkeys.android.polish.hindi.HindiPolisher
import com.flowkeys.android.polish.normalizers.IndianNumberNormalizer
import com.flowkeys.android.polish.normalizers.ScriptValidator
import com.flowkeys.android.polish.spans.ProtectedSpanManager
import com.flowkeys.android.snippets.SnippetEngine

/**
 * Main linguistic micro-polisher orchestrator.
 * Executes deterministically in < 3 milliseconds with zero neural memory overhead.
 */
object MicroPolisher {

    fun polish(
        rawText: String,
        language: Language,
        context: DictationContext? = null,
        smartMode: SmartMode = SmartMode.GENERAL
    ): String {
        if (rawText.isBlank()) return ""
        if (smartMode == SmartMode.RAW) return rawText.trim()

        // 0. Spoken Voice Commands & Punctuation Macros (Wispr Flow grade)
        var text = VoiceCommandEngine.process(rawText, language)
        if (text.isBlank()) return ""

        // 1. Snippet Expansion
        text = SnippetEngine.expand(text)

        // 2. Personal Vocabulary & Proper Noun Replacement
        text = PersonalDictionary.applyVocabulary(text)

        // 3. Conversational Self-Correction & Stutter Removal
        text = CorrectionResolver.resolve(text, language)

        // 4. Protected Spans Isolation (URLs, emails, phone numbers, currencies, code)
        val masked = ProtectedSpanManager.mask(text)
        text = masked.maskedText

        // 5. Language-Specific Linguistic Cleaner
        text = when (language) {
            Language.BENGALI -> BengaliPolisher.polish(text)
            Language.HINDI -> HindiPolisher.polish(text)
            Language.ENGLISH -> EnglishPolisher.polish(text)
        }

        // 6. Indian Numbering, Currency & Time Normalization
        text = IndianNumberNormalizer.normalize(text, language)

        // 7. Context-Aware Smart Formatting (Search / Messaging / Email / Developer)
        text = ContextAwareFormatter.format(text, language, context, smartMode)

        // 8. Restore Protected Spans
        text = ProtectedSpanManager.unmask(text, masked.placeholderMap)

        // 9. Length sanity guard: Prevent runaway expansions
        if (rawText.length > 5 && text.length > (rawText.length * 3 + 60)) {
            return rawText.trim()
        }

        // 10. Non-negotiable Script Validation Check
        if (!ScriptValidator.isValidScript(text, language)) {
            // If script validation fails unexpectedly, log or fallback to raw text to avoid corrupting output
            return rawText.trim()
        }

        return text.trim()
    }
}
