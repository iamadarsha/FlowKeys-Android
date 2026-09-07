package com.flowkeys.android.polish.transliteration

/**
 * Phonemic Hindi to Latin (Hinglish) Transliterator.
 * Produces natural, conversational Latin script for messaging (WhatsApp / Telegram / SMS).
 * e.g., "मैं कल ऑफिस जा रहा हूँ" -> "Main kal office ja raha hoon."
 */
object HindiLatinTransliterator {

    private val commonWordOverrides = mapOf(
        "मैं" to "main",
        "मेरा" to "mera",
        "मेरी" to "meri",
        "मेरे" to "mere",
        "मुझे" to "mujhe",
        "मुझको" to "mujhko",
        "हम" to "hum",
        "हमारा" to "hamara",
        "हमारी" to "hamari",
        "हमारे" to "hamare",
        "हमें" to "humein",
        "हमको" to "humko",
        "तू" to "tu",
        "तेरा" to "tera",
        "तेरी" to "teri",
        "तेरे" to "tere",
        "तुझे" to "tujhe",
        "तुम" to "tum",
        "तुम्हारा" to "tumhara",
        "तुम्हारी" to "tumhari",
        "तुम्हारे" to "tumhare",
        "तुम्हें" to "tumhein",
        "आप" to "aap",
        "आपका" to "aapka",
        "आपकी" to "aapki",
        "आपके" to "aapke",
        "आपको" to "aapko",
        "वह" to "woh",
        "वो" to "woh",
        "उसका" to "uska",
        "उसकी" to "uski",
        "उसके" to "uske",
        "उसे" to "use",
        "उसको" to "usko",
        "वे" to "ve",
        "उनका" to "unka",
        "उनकी" to "unki",
        "उनके" to "unke",
        "उन्हें" to "unhein",
        "यह" to "yeh",
        "ये" to "ye",
        "इसका" to "iska",
        "इसकी" to "iski",
        "इसके" to "iske",
        "इसे" to "ise",
        "इसको" to "isko",
        "क्या" to "kya",
        "क्यों" to "kyun",
        "कहाँ" to "kahan",
        "कहा" to "kaha",
        "कब" to "kab",
        "कैसे" to "kaise",
        "कैसा" to "kaisa",
        "कैसी" to "kaisi",
        "कौन" to "kaun",
        "किसे" to "kise",
        "किसका" to "kiska",
        "कितना" to "kitna",
        "कितनी" to "kitni",
        "कितने" to "kitne",
        "कल" to "kal",
        "आज" to "aaj",
        "परसों" to "parso",
        "सुबह" to "subah",
        "दोपहर" to "dopahar",
        "शाम" to "shaam",
        "रात" to "raat",
        "अभी" to "abhi",
        "तब" to "tab",
        "बाद" to "baad",
        "में" to "mein",
        "पे" to "pe",
        "पर" to "par",
        "से" to "se",
        "को" to "ko",
        "तक" to "tak",
        "लिए" to "liye",
        "साथ" to "saath",
        "घर" to "ghar",
        "ऑफिस" to "office",
        "दुकान" to "dukaan",
        "काम" to "kaam",
        "बात" to "baat",
        "फोन" to "phone",
        "मैसेज" to "message",
        "व्हाट्सएप" to "whatsapp",
        "व्हाट्सऐप" to "whatsapp",
        "ईमेल" to "email",
        "गाड़ी" to "gaadi",
        "रास्ता" to "raasta",
        "ट्रैफिक" to "traffic",
        "ट्रेन" to "train",
        "बस" to "bus",
        "मेट्रो" to "metro",
        "पैसा" to "paisa",
        "पैसे" to "paise",
        "रुपये" to "rupaye",
        "अच्छा" to "achha",
        "अच्छी" to "achhi",
        "अच्छे" to "achhe",
        "बुरा" to "bura",
        "सही" to "sahi",
        "गलत" to "galat",
        "ठीक" to "theek",
        "हाँ" to "haan",
        "नहीं" to "nahi",
        "ना" to "na",
        "मत" to "mat",
        "बहुत" to "bahut",
        "ज्यादा" to "jyada",
        "कम" to "kam",
        "थोड़ा" to "thoda",
        "थोड़ी" to "thodi",
        "जल्दी" to "jaldi",
        "धीरे" to "dheere",
        "भाई" to "bhai",
        "दोस्त" to "dost",
        "यार" to "yaar",
        "नमस्ते" to "namaste",
        "नमस्कार" to "namaskar",
        "धन्यवाद" to "dhanyawad",
        "शुक्रिया" to "shukriya",
        "अलविदा" to "alvida",
        "है" to "hai",
        "हैं" to "hain",
        "हूँ" to "hoon",
        "हो" to "ho",
        "था" to "tha",
        "थी" to "thi",
        "थे" to "the",
        "होगा" to "hoga",
        "होगी" to "hogi",
        "होंगे" to "honge",
        "आ" to "aa",
        "आओ" to "aao",
        "आइए" to "aaiye",
        "आया" to "aaya",
        "आयी" to "aayi",
        "आए" to "aaye",
        "आऊंगा" to "aaunga",
        "आऊँगा" to "aaunga",
        "आएगी" to "aaegi",
        "आएंगे" to "aaenge",
        "जा" to "ja",
        "जाओ" to "jao",
        "जाइए" to "jaiye",
        "गया" to "gaya",
        "गयी" to "gayi",
        "गए" to "gaye",
        "जाऊंगा" to "jaunga",
        "जाऊंगी" to "jaungi",
        "जाएगा" to "jaega",
        "जाएंगे" to "jaenge",
        "कर" to "kar",
        "करो" to "karo",
        "कीजिए" to "kijiye",
        "किया" to "kiya",
        "की" to "ki",
        "किए" to "kiye",
        "करूंगा" to "karunga",
        "करूंगी" to "karungi",
        "करेगा" to "karega",
        "करेंगे" to "karenge",
        "रहा" to "raha",
        "रही" to "rahi",
        "रहे" to "rahe",
        "बोल" to "bol",
        "बोलो" to "bolo",
        "बोलिए" to "boliye",
        "बोला" to "bola",
        "सुन" to "sun",
        "सुनो" to "suno",
        "सुनिए" to "suniye",
        "सुना" to "suna",
        "देख" to "dekh",
        "देखो" to "dekho",
        "देखिए" to "dekhiye",
        "देखा" to "dekha",
        "खा" to "kha",
        "खाओ" to "khao",
        "खाइए" to "khaiye",
        "खाया" to "khaya",
        "पी" to "pee",
        "पिओ" to "piyo",
        "पीजिए" to "peejiye",
        "पिया" to "piya",
        "ले" to "le",
        "लो" to "lo",
        "लीजिए" to "lijiye",
        "लिया" to "liya",
        "दे" to "de",
        "दो" to "do",
        "दीजिए" to "deejiye",
        "दिया" to "diya",
        "चाहिए" to "chahiye",
        "सकता" to "sakta",
        "सकती" to "sakti",
        "सकते" to "sakte",
        "पता" to "pata",
        "मालूम" to "maalum",
        "पहुँच" to "pahunch",
        "पहुँचूँगा" to "pahunchunga",
        "पहुँचा" to "pahuncha",
        "पहुँचेंगे" to "pahunchenge"
    )

    private val independentVowels = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u",
        'ऊ' to "oo", 'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au"
    )

    private val vowelSigns = mapOf(
        'ा' to "a", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
        'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
        'ं' to "n", 'ँ' to "n", 'ः' to "h"
    )

    private val consonants = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v", 'श' to "sh",
        'ष' to "sh", 'स' to "s", 'ह' to "h",
        '\u0958' to "q", '\u0959' to "kh", '\u095A' to "gh", '\u095B' to "z",
        '\u095C' to "r", '\u095D' to "rh", '\u095E' to "f", '\u095F' to "y"
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
            if (consonants.containsKey(c) || c == 'ज' || c == 'फ' || c == 'ड' || c == 'ढ') {
                var consonantStr = consonants[c] ?: ""

                // Check for combining Nukta (U+093C)
                if (i + 1 < len && cleanWord[i + 1] == '\u093C') {
                    consonantStr = when (c) {
                        'ज' -> "z"
                        'फ' -> "f"
                        'ड' -> "r"
                        'ढ' -> "rh"
                        'क' -> "q"
                        'ख' -> "kh"
                        'ग' -> "gh"
                        else -> consonantStr
                    }
                    i++ // skip nukta
                }

                sb.append(consonantStr)

                // Check next character
                if (i + 1 < len) {
                    val next = cleanWord[i + 1]
                    if (next == '्') {
                        // Halant / Virama (conjunct) -> suppress implicit vowel 'a'
                        i += 2
                        continue
                    } else if (vowelSigns.containsKey(next)) {
                        val matra = vowelSigns[next] ?: ""
                        // End-of-word 'ी' is typically transliterated as 'i' (e.g. gari, dosti)
                        if (next == 'ी' && i + 2 >= len) {
                            sb.append("i")
                        } else {
                            sb.append(matra)
                        }
                        i += 2
                        continue
                    } else if (consonants.containsKey(next) || independentVowels.containsKey(next)) {
                        // Word medial implicit 'a'
                        if (i + 2 >= len && (c == 'र' || c == 'ल' || c == 'म' || c == 'न' || c == 'त' || c == 'क' || c == 'स')) {
                            // In Hindi, final consonants exhibit schwa deletion (e.g. kal, ghar, kaam)
                        } else {
                            sb.append("a")
                        }
                    }
                }
                i++
                continue
            }

            // Pass through Latin characters, numbers, and punctuation
            sb.append(c)
            i++
        }

        return sb.toString()
    }
}
