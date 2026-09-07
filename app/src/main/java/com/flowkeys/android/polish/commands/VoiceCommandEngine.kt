package com.flowkeys.android.polish.commands

import com.flowkeys.android.core.model.Language

/**
 * Enterprise-grade voice command and punctuation macro engine.
 * Converts spoken formatting cues ("new line", "comma", "bullet point", "scratch that")
 * into structural text transformations across English, Bengali, and Hindi.
 */
object VoiceCommandEngine {

    fun process(text: String, language: Language): String {
        if (text.isBlank()) return text

        var result = text

        when (language) {
            Language.ENGLISH -> {
                result = processEnglishCommands(result)
            }
            Language.BENGALI -> {
                result = processBengaliCommands(result)
            }
            Language.HINDI -> {
                result = processHindiCommands(result)
            }
        }

        return result
    }

    private fun processEnglishCommands(text: String): String {
        var res = text
        // Erasure / Backtrack
        val englishBacktrackRegex = Regex("(?i)\\b(scratch that|delete that|undo that|start again|cancel that)\\b")
        if (res.contains(englishBacktrackRegex)) {
            val parts = res.split(englishBacktrackRegex)
            res = if (parts.size > 1 && parts.last().trim().isNotEmpty()) {
                parts.last().trim()
            } else {
                ""
            }
        }

        res = res.replace(Regex("(?i)\\s*\\bnew paragraph\\b\\s*"), "\n\n")
        res = res.replace(Regex("(?i)\\s*\\b(new line|next line)\\b\\s*"), "\n")
        res = res.replace(Regex("(?i)\\s*\\b(bullet point|bullet)\\b\\s*"), "\n• ")
        res = res.replace(Regex("(?i)\\s*\\bcomma\\b"), ",")
        res = res.replace(Regex("(?i)\\s*\\b(full stop|period)\\b"), ".")
        res = res.replace(Regex("(?i)\\s*\\b(question mark)\\b"), "?")
        res = res.replace(Regex("(?i)\\s*\\b(exclamation mark|exclamation point)\\b"), "!")
        res = res.replace(Regex("(?i)\\s*\\bcolon\\b"), ":")
        res = res.replace(Regex("(?i)\\s*\\bsemicolon\\b"), ";")
        res = res.replace(Regex("(?i)\\s*\\b(close quotes?|end quotes?)\\b"), "\"")
        res = res.replace(Regex("(?i)\\b(open quotes?|quote)\\b\\s*"), "\"")
        res = res.replace(Regex("(?i)\\b(open parenthesis|open bracket)\\b\\s*"), "(")
        res = res.replace(Regex("(?i)\\s*\\b(close parenthesis|close bracket)\\b"), ")")
        return res
    }

    private fun processBengaliCommands(text: String): String {
        var res = text
        val bengaliBacktrackRegex = Regex("(মুছে ফেলো|ভুল হয়েছে|মুছে দাও|ক্যানসেল|নতুন করে বলছি|আবার বলছি|আবার শুরু করি)")
        if (res.contains(bengaliBacktrackRegex)) {
            val parts = res.split(bengaliBacktrackRegex)
            res = if (parts.size > 1 && parts.last().trim().isNotEmpty()) {
                parts.last().trim()
            } else {
                ""
            }
        }

        res = res.replace(Regex("\\s*(নতুন প্যারাগ্রাফ|পরের প্যারাগ্রাফ)\\s*"), "\n\n")
        res = res.replace(Regex("\\s*(নতুন লাইন|পরের লাইন)\\s*"), "\n")
        res = res.replace(Regex("\\s*(বুলেট পয়েন্ট|বুলেট)\\s*"), "\n• ")
        res = res.replace(Regex("\\s*কমা"), ",")
        res = res.replace(Regex("\\s*(দাঁড়ি|দাড়ি)"), "।")
        res = res.replace(Regex("\\s*(প্রশ্নচিহ্ন|জিজ্ঞাসাচিহ্ন)"), "?")
        res = res.replace(Regex("\\s*(বিস্ময়সূচক|বিস্ময়বোধক চিহ্ন)"), "!")
        res = res.replace(Regex("\\s*কোলন"), ":")
        res = res.replace(Regex("\\s*সেমিকোলন"), ";")
        res = res.replace(Regex("(বন্ধনী শুরু|ব্র্যাকেট শুরু)\\s*"), "(")
        res = res.replace(Regex("\\s*(বন্ধনী শেষ|ব্র্যাকেট শেষ)"), ")")
        return res
    }

    private fun processHindiCommands(text: String): String {
        var res = text
        val hindiBacktrackRegex = Regex("(हटाओ|गलत हो गया|मिटाओ|कैंसिल|फिर से बोल रहा हूँ|शुरू से bol raha hu|शुरू से)")
        if (res.contains(hindiBacktrackRegex)) {
            val parts = res.split(hindiBacktrackRegex)
            res = if (parts.size > 1 && parts.last().trim().isNotEmpty()) {
                parts.last().trim()
            } else {
                ""
            }
        }

        res = res.replace(Regex("\\s*(नया पैराग्राफ|अगला पैराग्राफ)\\s*"), "\n\n")
        res = res.replace(Regex("\\s*(नई लाइन|अगली लाइन)\\s*"), "\n")
        res = res.replace(Regex("\\s*(बुलेट पॉइंट|बुलेट)\\s*"), "\n• ")
        res = res.replace(Regex("\\s*(कॉमा|अल्पविराम)"), ",")
        res = res.replace(Regex("\\s*(पूर्ण विराम|पूर्णविराम)"), "।")
        res = res.replace(Regex("\\s*(प्रश्नवाचक चिन्ह|प्रश्नवाचक)"), "?")
        res = res.replace(Regex("\\s*(विस्मयादिबोधक चिन्ह|विस्मयादिबोधक)"), "!")
        res = res.replace(Regex("\\s*कोलन"), ":")
        res = res.replace(Regex("\\s*सेमीकोलन"), ";")
        res = res.replace(Regex("(ब्रैकेट शुरू)\\s*"), "(")
        res = res.replace(Regex("\\s*(ब्रैकेट बंद)"), ")")
        return res
    }
}
