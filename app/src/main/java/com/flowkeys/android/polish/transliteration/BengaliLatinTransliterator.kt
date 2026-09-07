package com.flowkeys.android.polish.transliteration

/**
 * Phonemic Bengali to Latin (Benglish) Transliterator.
 * Produces natural, youth-standard Kolkata/Bengal conversational Latin script.
 * e.g., "আমি কাল অফিসে যাব" -> "Ami kal office e jabo."
 */
object BengaliLatinTransliterator {

    private val commonWordOverrides = mapOf(
        "আমি" to "ami",
        "আমার" to "amar",
        "আমাকে" to "amake",
        "আমরা" to "amra",
        "আমাদের" to "amader",
        "তুমি" to "tumi",
        "তোমার" to "tomar",
        "তোমাকে" to "tomake",
        "তোমরা" to "tomra",
        "আপনি" to "apni",
        "আপনার" to "apnar",
        "আপনাকে" to "apnake",
        "সে" to "she",
        "তার" to "tar",
        "তাকে" to "take",
        "তারা" to "tara",
        "এটা" to "eta",
        "ওটা" to "ota",
        "এই" to "ei",
        "ওই" to "oi",
        "কী" to "ki",
        "কি" to "ki",
        "কেন" to "keno",
        "কোথায়" to "kothay",
        "কখন" to "kokhon",
        "কিভাবে" to "kibhabe",
        "কেমন" to "kemon",
        "কে" to "ke",
        "কাকে" to "kake",
        "কার" to "kar",
        "কাল" to "kal",
        "কালকে" to "kalke",
        "আজ" to "aaj",
        "আজকে" to "aajke",
        "পরশু" to "porshu",
        "সকালে" to "sokale",
        "বিকালে" to "bikale",
        "সন্ধ্যায়" to "sondhyay",
        "রাতে" to "raate",
        "এখন" to "ekhon",
        "তখন" to "tokhon",
        "পরে" to "pore",
        "একটু" to "ektu",
        "দেরি" to "deri",
        "তাড়াতাড়ি" to "taratari",
        "বাড়ি" to "bari",
        "বাড়িতে" to "barite",
        "অফিস" to "office",
        "অফিসে" to "office e",
        "মিটিং" to "meeting",
        "মিটিংয়ে" to "meeting e",
        "কাজ" to "kaaj",
        "কাজে" to "kaje",
        "ফোন" to "phone",
        "ফোনে" to "phone e",
        "মেসেজ" to "message",
        "হোয়াটসঅ্যাপ" to "whatsapp",
        "হোয়াটসঅ্যাপে" to "whatsapp e",
        "ইমেল" to "email",
        "ভালো" to "bhalo",
        "খারাপ" to "kharap",
        "সুন্দর" to "sundor",
        "খুব" to "khub",
        "অনেক" to "onek",
        "ধন্যবাদ" to "dhonnobad",
        "নমস্কার" to "nomoshkar",
        "সুপ্রভাত" to "suprobhat",
        "শুভরাত্রি" to "shubhoratri",
        "আছি" to "achhi",
        "আছো" to "achho",
        "আছেন" to "achhen",
        "যাচ্ছি" to "jachhi",
        "যাচ্ছ" to "jachho",
        "যাচ্ছেন" to "jachhen",
        "যাব" to "jabo",
        "যাবেন" to "jaben",
        "গেলাম" to "gelam",
        "করছি" to "korchi",
        "করছ" to "korcho",
        "করছে" to "korche",
        "করবেন" to "korben",
        "করব" to "korbo",
        "বলছি" to "bolchi",
        "বলব" to "bolbo",
        "বলবেন" to "bolben",
        "আসছি" to "ashchi",
        "আসবে" to "ashbe",
        "আসবেন" to "ashben",
        "পৌঁছে" to "pouchhe",
        "পৌঁছাব" to "pouchhabo",
        "খাব" to "khabo",
        "খাচ্ছি" to "khachhi",
        "খেয়েছি" to "kheyechhi",
        "রাস্তায়" to "rastay",
        "ট্রাফিক" to "traffic",
        "ট্রেন" to "train",
        "বাস" to "bus",
        "মেট্রো" to "metro",
        "গাড়ি" to "gari",
        "টাকা" to "taka",
        "পয়সা" to "poisa",
        "হ্যাঁ" to "haa",
        "না" to "na",
        "ঠিক" to "thik",
        "সব" to "shob",
        "সবাই" to "shobai",
        "সবাইকে" to "shobai ke",
        "শান্ত" to "shanto",
        "ভাই" to "bhai",
        "দাদা" to "dada",
        "দিদি" to "didi",
        "বন্ধু" to "bondhu"
    )

    private val independentVowels = mapOf(
        'অ' to "o", 'আ' to "a", 'ই' to "i", 'ঈ' to "i", 'উ' to "u",
        'ঊ' to "u", 'ঋ' to "ri", 'এ' to "e", 'ঐ' to "oi", 'ও' to "o", 'ঔ' to "ou"
    )

    private val vowelSigns = mapOf(
        'া' to "a", 'ি' to "i", 'ী' to "i", 'ু' to "u", 'ূ' to "u",
        'ৃ' to "ri", 'ে' to "e", 'ৈ' to "oi", 'ো' to "o", 'ৌ' to "ou"
    )

    private val consonants = mapOf(
        'ক' to "k", 'খ' to "kh", 'গ' to "g", 'ঘ' to "gh", 'ঙ' to "ng",
        'চ' to "ch", 'ছ' to "chh", 'জ' to "j", 'ঝ' to "jh", 'ঞ' to "n",
        'ট' to "t", 'ঠ' to "th", 'ড' to "d", 'ঢ' to "dh", 'ণ' to "n",
        'ত' to "t", 'থ' to "th", 'দ' to "d", 'ধ' to "dh", 'ন' to "n",
        'প' to "p", 'ফ' to "ph", 'ব' to "b", 'ভ' to "bh", 'ম' to "m",
        'য' to "j", 'র' to "r", 'ল' to "l", 'শ' to "sh", 'ষ' to "sh",
        'স' to "s", 'হ' to "h",
        '\u09DC' to "r", '\u09DD' to "rh", '\u09DF' to "y",
        '\u09CE' to "t", '\u0982' to "ng", '\u0983' to "h", '\u0981' to ""
    )

    fun transliterate(text: String): String {
        if (text.isBlank()) return text

        val punctuation = when {
            text.endsWith("?") -> "?"
            text.endsWith("!") -> "!"
            text.endsWith("।") || text.endsWith(".") -> "."
            else -> ""
        }

        val clean = text.removeSuffix("।").removeSuffix(".").removeSuffix("?").removeSuffix("!").trim()
        val words = clean.split(Regex("\\s+"))

        val resultWords = words.map { word ->
            transliterateWord(word)
        }

        val combined = resultWords.joinToString(" ").trim()
        if (combined.isEmpty()) return ""

        val capitalized = combined.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        return if (punctuation.isNotEmpty()) "$capitalized$punctuation" else "$capitalized."
    }

    private fun transliterateWord(word: String): String {
        val cleanWord = word.trim()
        val override = commonWordOverrides[cleanWord] ?: commonWordOverrides[cleanWord.lowercase()]
        if (override != null) return override

        val sb = StringBuilder()
        var i = 0
        val len = cleanWord.length

        while (i < len) {
            val c = cleanWord[i]

            // 1. Independent Vowel
            if (independentVowels.containsKey(c)) {
                sb.append(independentVowels[c])
                i++
                continue
            }

            // 2. Consonant
            if (consonants.containsKey(c) || c == 'ড' || c == 'ঢ' || c == 'য') {
                var consonantStr = consonants[c] ?: ""

                // Check for Bengali Nukta (U+09BC)
                if (i + 1 < len && cleanWord[i + 1] == '\u09BC') {
                    consonantStr = when (c) {
                        'ড' -> "r"
                        'ঢ' -> "rh"
                        'য' -> "y"
                        else -> consonantStr
                    }
                    i++
                }

                sb.append(consonantStr)

                // Check next character
                if (i + 1 < len) {
                    val next = cleanWord[i + 1]
                    if (next == '্') {
                        // Hasanta (conjunct) -> skip implicit vowel
                        i += 2
                        continue
                    } else if (vowelSigns.containsKey(next)) {
                        sb.append(vowelSigns[next])
                        i += 2
                        continue
                    } else if (consonants.containsKey(next) || independentVowels.containsKey(next)) {
                        // Word medial implicit 'o' or 'a'
                        if (i + 2 >= len && (c == 'র' || c == 'ল' || c == 'ম' || c == 'ন' || c == 'ত' || c == 'ক')) {
                            // In Bengali, final consonants usually have schwa deletion
                        } else {
                            sb.append("o")
                        }
                    }
                }
                i++
                continue
            }

            // Pass through Latin letters, digits, punctuation
            sb.append(c)
            i++
        }

        return sb.toString()
    }
}
