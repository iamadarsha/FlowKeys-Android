package com.flowkeys.android.polish.normalizers

import com.flowkeys.android.core.model.Language

/**
 * Normalizes Indian currency, numbering systems (Lakhs, Crores), and spoken time expressions.
 */
object IndianNumberNormalizer {

    private val englishNumberWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "twenty" to "20", "thirty" to "30", "forty" to "40", "fifty" to "50",
        "hundred" to "100", "thousand" to "1000", "lakh" to "100000", "crore" to "10000000"
    )

    fun normalize(text: String, language: Language): String {
        var result = text

        // 1. Time Normalization: e.g. "4 30 pm" -> "4:30 PM"
        result = result.replace(Regex("(?i)\\b(\\d{1,2})\\s+(\\d{2})\\s*(am|pm)\\b")) { match ->
            "${match.groupValues[1]}:${match.groupValues[2]} ${match.groupValues[3].uppercase()}"
        }

        // 2. Rupee / Currency normalization
        result = result.replace(Regex("(?i)\\b(\\d+)\\s*(rupees|rupee|rs|inr)\\b")) { match ->
            "₹${formatIndianGrouping(match.groupValues[1])}"
        }

        // 3. Indian Lakhs and Crores text normalization in English context
        result = result.replace(Regex("(?i)\\b(\\d+)\\s*lakh\\b")) { match ->
            "${match.groupValues[1]},00,000"
        }
        result = result.replace(Regex("(?i)\\b(\\d+)\\s*crore\\b")) { match ->
            "${match.groupValues[1]},00,00,000"
        }

        // 4. Bengali Currency & Numbering
        if (language == Language.BENGALI) {
            result = result.replace(Regex("(\\d+)\\s*টাকা")) { match ->
                "₹${match.groupValues[1]}"
            }
            result = result.replace(Regex("(\\d+)\\s*লাখ")) { match ->
                "${match.groupValues[1]},০০,০০০"
            }
            result = result.replace(Regex("(\\d+)\\s*কোটি")) { match ->
                "${match.groupValues[1]},০০,০০,০০০"
            }
        }

        // 5. Hindi Currency & Numbering
        if (language == Language.HINDI) {
            result = result.replace(Regex("(\\d+)\\s*रुपये")) { match ->
                "₹${match.groupValues[1]}"
            }
            result = result.replace(Regex("(\\d+)\\s*लाख")) { match ->
                "${match.groupValues[1]},00,000"
            }
            result = result.replace(Regex("(\\d+)\\s*करोड़")) { match ->
                "${match.groupValues[1]},00,00,000"
            }
        }

        return result
    }

    /**
     * Formats an integer string according to the Indian numbering system (e.g. 1,00,000).
     */
    fun formatIndianGrouping(numberStr: String): String {
        val clean = numberStr.trimStart('0')
        if (clean.length <= 3) return clean.ifEmpty { "0" }

        val lastThree = clean.substring(clean.length - 3)
        val rest = clean.substring(0, clean.length - 3)

        val groups = mutableListOf<String>()
        var i = rest.length
        while (i > 0) {
            val start = (i - 2).coerceAtLeast(0)
            groups.add(0, rest.substring(start, i))
            i -= 2
        }

        return groups.joinToString(",") + ",$lastThree"
    }
}
