package com.flowkeys.android.polish.bengali

import com.flowkeys.android.polish.translation.OfflineIndicLexicon

/**
 * Specialized deterministic cleaner for West Bengal / Kolkata Bengali.
 *
 * Capabilities:
 * - Strips West Bengal colloquial hesitation fillers ("আসলে", "ওই যে", etc.)
 * - Context-sensitive "মানে" handling — preserves when semantic ("এর মানে কি")
 * - Normalizes 40+ English loanwords into phonemic Bengali script (Benglish support)
 * - Appends sentence-terminal Dari ("।") or question mark ("?")
 * - Handles code-mixed Bengali+English text gracefully
 */
object BengaliPolisher {

    private val fillers = listOf(
        "আসলে",
        "ওই যে",
        "কী যেন বলে",
        "বুঝেছো তো",
        "তাই না",
        "ইয়ে",
        "মানে আর কি",
        "যাই হোক",
        "ধর",
        "হ্যাঁ মানে",
        "um",
        "uh",
        "ah",
        "hmm",
        "like",
        "you know",
        "actually",
        "basically"
    )

    // Expanded Benglish loanword mappings — 40+ common English words used in Bengali speech
    private val loanwordsMap = mapOf(
        "(?i)\\bmeeting\\b" to "মিটিং",
        "(?i)\\boffice\\b" to "অফিস",
        "(?i)\\btrain\\b" to "ট্রেন",
        "(?i)\\bstation\\b" to "স্টেশন",
        "(?i)\\bcancel(led)?\\b" to "ক্যানসেল",
        "(?i)\\bproblem\\b" to "প্রবলেম",
        "(?i)\\bmessage\\b" to "মেসেজ",
        "(?i)\\bcall\\b" to "কল",
        "(?i)\\bboss\\b" to "বস",
        "(?i)\\bclient\\b" to "ক্লায়েন্ট",
        "(?i)\\bphone\\b" to "ফোন",
        "(?i)\\bemail\\b" to "ইমেল",
        "(?i)\\bfile\\b" to "ফাইল",
        "(?i)\\bcomputer\\b" to "কম্পিউটার",
        "(?i)\\blaptop\\b" to "ল্যাপটপ",
        "(?i)\\binternet\\b" to "ইন্টারনেট",
        "(?i)\\bwhatsapp\\b" to "হোয়াটসঅ্যাপ",
        "(?i)\\bschool\\b" to "স্কুল",
        "(?i)\\bcollege\\b" to "কলেজ",
        "(?i)\\buniversity\\b" to "ইউনিভার্সিটি",
        "(?i)\\bhospital\\b" to "হাসপাতাল",
        "(?i)\\bdoctor\\b" to "ডাক্তার",
        "(?i)\\bmedicine\\b" to "মেডিসিন",
        "(?i)\\bmarket\\b" to "মার্কেট",
        "(?i)\\bshop(ping)?\\b" to "শপিং",
        "(?i)\\bticket\\b" to "টিকিট",
        "(?i)\\bbus\\b" to "বাস",
        "(?i)\\btaxi\\b" to "ট্যাক্সি",
        "(?i)\\bauto\\b" to "অটো",
        "(?i)\\bflight\\b" to "ফ্লাইট",
        "(?i)\\bairport\\b" to "এয়ারপোর্ট",
        "(?i)\\bhotel\\b" to "হোটেল",
        "(?i)\\brestaurant\\b" to "রেস্তোরাঁ",
        "(?i)\\border\\b" to "অর্ডার",
        "(?i)\\bdelivery\\b" to "ডেলিভারি",
        "(?i)\\bpayment\\b" to "পেমেন্ট",
        "(?i)\\bproject\\b" to "প্রজেক্ট",
        "(?i)\\breport\\b" to "রিপোর্ট",
        "(?i)\\bdeadline\\b" to "ডেডলাইন",
        "(?i)\\bupdate\\b" to "আপডেট",
        "(?i)\\bdownload\\b" to "ডাউনলোড",
        "(?i)\\bpassword\\b" to "পাসওয়ার্ড",
        "(?i)\\bsorry\\b" to "সরি",
        "(?i)\\bthanks\\b" to "থ্যাংকস",
        "(?i)\\bplease\\b" to "প্লিজ",
        "(?i)\\bokay\\b" to "ওকে",
        "(?i)\\bok\\b" to "ওকে",
        "(?i)\\bbye\\b" to "বাই"
    )

    private val interrogatives = listOf("কেন", "কোথায়", "কখন", "কী", "কি", "কীভাবে", "কে", "কার", "কোন", "কত")

    fun polish(rawText: String): String {
        var text = rawText.trim()
        if (text.isEmpty()) return text

        // 0. West Bengal colloquial phrase normalization from OfflineIndicLexicon
        text = OfflineIndicLexicon.normalizeWestBengalBengali(text)

        // 1. Strip standard hesitation fillers with robust Unicode boundaries
        for (filler in fillers) {
            val pattern = if (filler.all { it.code < 128 }) {
                "(?i)\\b$filler\\b"
            } else {
                "(?:^|(?<=\\s))${Regex.escape(filler)}(?=\\s|[।,?!]|$)"
            }
            text = text.replace(Regex(pattern), "")
        }

        // 2. Context-sensitive "মানে" stripper
        //    ONLY strip when used as a hesitation filler (sentence-start or after clause-ending punctuation)
        //    PRESERVE when used semantically: "এর মানে", "তার মানে", "এটার মানে", "কী মানে" etc.
        text = text.replace(Regex("^মানে(?=\\s|[।,?!])"), "")
        text = text.replace(Regex("([।,?!])\\s*মানে(?=\\s|[।,?!]|$)"), "$1")

        // 3. Phonemic loanword mapping (Benglish normalization)
        for ((pattern, replacement) in loanwordsMap) {
            text = text.replace(Regex(pattern), replacement)
        }

        // 4. Collapse extra whitespaces
        text = text.replace(Regex("\\s{2,}"), " ").trim()

        // 5. Bengali terminal punctuation (Dari or Question Mark)
        if (text.isNotEmpty() && !text.endsWith("।") && !text.endsWith("?") && !text.endsWith("!") && !text.endsWith(".")) {
            val words = text.split(Regex("\\s+")).map { it.trim().removeSuffix("।").removeSuffix("?").removeSuffix(",") }
            val isQuestion = words.any { it in interrogatives }
            text = if (isQuestion) "$text?" else "$text।"
        }

        return text
    }
}
