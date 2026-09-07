package com.flowkeys.android.polish.translation

import com.flowkeys.android.core.model.Language
import java.util.Locale

/**
 * Enterprise-grade Offline Indic Lexicon and Multi-dialect Semantic Dictionary.
 *
 * Pre-indexes:
 * 1. Deep West Bengal / Kolkata Colloquial Bengali conversational expressions
 * 2. Conversational Hindi & North Indian colloquial idioms
 * 3. Modern Indian English, Internet slang, WhatsApp shorthand, and Tech jargon
 * 4. High-performance Trie engine for sub-millisecond offline translation & phrase correction
 */
object OfflineIndicLexicon {

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var translation: String? = null
        var isEndOfWord: Boolean = false
    }

    private class PhraseTrie {
        private val root = TrieNode()

        fun normalizeIndicUnicode(input: String): String {
            return input.lowercase(Locale.ROOT).trim()
                .replace("\u09AF\u09BC", "\u09DF") // Bengali য় -> য়
                .replace("\u09A1\u09BC", "\u09DC") // Bengali ড় -> ড়
                .replace("\u09A2\u09BC", "\u09DD") // Bengali ঢ় -> ঢ়
        }

        fun insert(phrase: String, translation: String) {
            var current = root
            val clean = normalizeIndicUnicode(phrase)
            for (ch in clean) {
                current = current.children.getOrPut(ch) { TrieNode() }
            }
            current.isEndOfWord = true
            current.translation = translation
        }

        fun search(phrase: String): String? {
            var current = root
            val clean = normalizeIndicUnicode(phrase)
            for (ch in clean) {
                current = current.children[ch] ?: return null
            }
            return if (current.isEndOfWord) current.translation else null
        }
    }

    // Directional Tries for instantaneous sub-1ms search
    private val bengaliToEnglishTrie = PhraseTrie()
    private val hindiToEnglishTrie = PhraseTrie()
    private val englishToBengaliTrie = PhraseTrie()
    private val englishToHindiTrie = PhraseTrie()
    private val bengaliToHindiTrie = PhraseTrie()
    private val hindiToBengaliTrie = PhraseTrie()

    // Internet, WhatsApp, and Modern Indian Slang Map
    private val internetAndWhatsAppSlang = mapOf(
        "(?i)\\bwfh\\b" to "working from home",
        "(?i)\\basap\\b" to "as soon as possible",
        "(?i)\\bomw\\b" to "on my way",
        "(?i)\\bfyi\\b" to "for your information",
        "(?i)\\bbrb\\b" to "be right back",
        "(?i)\\bttyl\\b" to "talk to you later",
        "(?i)\\btbh\\b" to "to be honest",
        "(?i)\\bidk\\b" to "I don't know",
        "(?i)\\bnp\\b" to "no problem",
        "(?i)\\blgtm\\b" to "looks good to me",
        "(?i)\\beod\\b" to "end of day",
        "(?i)\\beta\\b" to "estimated time of arrival",
        "(?i)\\brsvp\\b" to "please confirm attendance",
        "(?i)\\bdm\\b" to "direct message",
        "(?i)\\bg2g\\b" to "got to go",
        "(?i)\\bprepone\\b" to "reschedule earlier",
        "(?i)\\bpreponed\\b" to "rescheduled earlier",
        "(?i)\\brevert back\\b" to "reply",
        "(?i)\\bdo the needful\\b" to "take the necessary action",
        "(?i)\\bout of station\\b" to "out of town",
        "(?i)\\bpassed out of college\\b" to "graduated from college",
        "(?i)\\bpassed out\\b" to "graduated",
        "(?i)\\bcousin brother\\b" to "cousin",
        "(?i)\\bcousin sister\\b" to "cousin",
        "(?i)\\bgood name\\b" to "name",
        "(?i)\\btell me one thing\\b" to "tell me",
        "(?i)\\blevel best\\b" to "best",
        "(?i)\\bbatchmate\\b" to "classmate",
        "(?i)\\bchalo let's go\\b" to "let's go",
        "(?i)\\bya ya\\b" to "yes yes",
        "(?i)\\bpakka\\b" to "confirmed",
        "(?i)\\bjugaad\\b" to "innovative quick fix",
        "(?i)\\bfundas\\b" to "fundamentals"
    )

    // Kolkata / West Bengal specific conversational normalizations
    private val westBengalColloquialFixes = mapOf(
        "(?i)\\boffice jachhi\\b" to "অফিসে যাচ্ছি",
        "(?i)\\bmeeting e achhi\\b" to "মিটিংয়ে আছি",
        "(?i)\\brastay achhi\\b" to "রাস্তায় আছি",
        "(?i)\\bphn korchi\\b" to "ফোন করছি",
        "(?i)\\bmsg pathiyechi\\b" to "মেসেজ পাঠিয়েছি",
        "(?i)\\bwp e pathao\\b" to "হোয়াটসঅ্যাপে পাঠাও",
        "(?i)\\bkoto baje\\b" to "কটা বাজে",
        "(?i)\\bkhawa hoyeche\\b" to "খাওয়া হয়েছে",
        "(?i)\\bcha khabe\\b" to "চা খাবে",
        "(?i)\\bektu por\\b" to "একটু পর",
        "(?i)\\bbari jacchi\\b" to "বাড়ি যাচ্ছি",
        "(?i)\\bmetro dhorechi\\b" to "মেট্রো ধরেছি",
        "(?i)\\btrain e uthechi\\b" to "ট্রেনে উঠেছি",
        "(?i)\\buber niyechi\\b" to "উবার নিয়েছি",
        "(?i)\\btaka pathiyechi\\b" to "টাকা পাঠিয়েছি",
        "(?i)\\bgpay korechi\\b" to "গুগল পে করেছি",
        "(?i)\\bchinta korona\\b" to "কোনো চিন্তা নেই",
        "(?i)\\bdhonnobad dada\\b" to "অনেক ধন্যবাদ দাদা"
    )

    init {
        loadBengaliEnglishDataset()
        loadHindiEnglishDataset()
        loadBengaliHindiDataset()
    }

    fun expandInternetSlang(text: String): String {
        if (text.isBlank()) return text
        var result = text
        for ((pattern, replacement) in internetAndWhatsAppSlang) {
            result = result.replace(Regex(pattern), replacement)
        }
        return result
    }

    fun normalizeWestBengalBengali(text: String): String {
        if (text.isBlank()) return text
        var result = text
        for ((pattern, replacement) in westBengalColloquialFixes) {
            result = result.replace(Regex(pattern), replacement)
        }
        return result
    }

    fun lookup(phrase: String, sourceLang: Language, targetLang: Language): String? {
        val clean = phrase.trim()
        if (clean.isBlank()) return null

        return when {
            sourceLang == Language.BENGALI && targetLang == Language.ENGLISH -> bengaliToEnglishTrie.search(clean)
            sourceLang == Language.ENGLISH && targetLang == Language.BENGALI -> englishToBengaliTrie.search(clean)
            sourceLang == Language.HINDI && targetLang == Language.ENGLISH -> hindiToEnglishTrie.search(clean)
            sourceLang == Language.ENGLISH && targetLang == Language.HINDI -> englishToHindiTrie.search(clean)
            sourceLang == Language.BENGALI && targetLang == Language.HINDI -> bengaliToHindiTrie.search(clean)
            sourceLang == Language.HINDI && targetLang == Language.BENGALI -> hindiToBengaliTrie.search(clean)
            else -> null
        }
    }

    private fun addPair(bn: String, en: String, hi: String) {
        bengaliToEnglishTrie.insert(bn, en)
        englishToBengaliTrie.insert(en, bn)

        hindiToEnglishTrie.insert(hi, en)
        englishToHindiTrie.insert(en, hi)

        bengaliToHindiTrie.insert(bn, hi)
        hindiToBengaliTrie.insert(hi, bn)
    }

    private fun loadBengaliEnglishDataset() {
        // --- 1. Kolkata Daily Greetings & Status ---
        addPair("নমস্কার", "Hello.", "नमस्ते")
        addPair("সুপ্রভাত", "Good morning", "शुभ प्रभात")
        addPair("শুভরাত্রি", "Good night", "शुभ रात्रि")
        addPair("কেমন আছেন?", "How are you?", "आप कैसे हैं?")
        addPair("কেমন আছো?", "How are you?", "तुम कैसे हो?")
        addPair("কি খবর?", "What's up?", "क्या खबर है?")
        addPair("সব ঠিকঠাক?", "Is everything alright?", "सब ठीक-ठाक है?")
        addPair("আমি ভালো আছি", "I am doing well", "मैं ठीक हूँ")
        addPair("অনেক ধন্যবাদ", "Thank you very much", "बहुत-बहुत धन्यवाद")
        addPair("ধন্যবাদ দাদা", "Thank you brother", "धन्यवाद भाई")
        addPair("ধন্যবাদ দিদি", "Thank you sister", "धन्यवाद दीदी")
        addPair("কিছু মনে করবেন না", "Please don't mind", "बुरा मत मानिए")
        addPair("দেখা হবে", "See you soon", "फिर मिलेंगे")
        addPair("কালকে দেখা হচ্ছে", "See you tomorrow", "कल मिलते हैं")
        addPair("ভালো থেকো", "Take care", "ख्याल रखना")
        addPair("নিজের খেয়াল রাখবেন", "Take care of yourself", "अपना ख्याल रखिएगा")

        // --- 2. Kolkata Commute, Transit & Locations ---
        addPair("আমি রাস্তায় আছি", "I am on the way", "मैं रास्ते में हूँ")
        addPair("অনেক ট্রাফিক আছে", "There is heavy traffic", "बहुत ट्रैफिक है")
        addPair("রাস্তায় খুব জ্যাম", "Traffic is jammed", "रास्ते में बहुत जाम है")
        addPair("একটু দেরি হবে", "I will be a little late", "थोड़ी देर हो जाएगी")
        addPair("আমি ১০ মিনিটে আসছি", "I am coming in 10 minutes", "मैं 10 मिनट में आ रहा हूँ")
        addPair("আমি পৌঁছে গেছি", "I have arrived", "मैं पहुँच गया हूँ")
        addPair("কোথায় আছো?", "Where are you?", "कहाँ हो?")
        addPair("কোথায় দেখা করব?", "Where shall we meet?", "कहाँ मिलेंगे?")
        addPair("বাড়ি যাচ্ছি", "Going home", "घर जा रहा हूँ")
        addPair("অফিসে যাচ্ছি", "Going to office", "ऑफिस जा रहा हूँ")
        addPair("অফিসে পৌঁছে গেছি", "Arrived at office", "ऑफिस पहुँच गया हूँ")
        addPair("মেট্রো ধরে নিয়েছি", "I have boarded the metro", "मेट्रो पकड़ ली है")
        addPair("ট্রেনে উঠেছি", "I have boarded the train", "ट्रेन में चढ़ गया हूँ")
        addPair("ক্যাব বুক করেছি", "I have booked a cab", "कैब बुक कर ली है")
        addPair("উবার নিয়েছি", "I took an Uber", "उबर ले ली है")
        addPair("বাসে আছি", "I am on the bus", "बस में हूँ")
        addPair("বিমানবন্দরে আছি", "I am at the airport", "एयरपोर्ट पर हूँ")
        addPair("স্টেশনে আছি", "I am at the railway station", "स्टेशन पर हूँ")

        // --- 3. Office, Work & Tech Messaging ---
        addPair("মিটিং শুরু হয়ে গেছে", "The meeting has started", "मीटिंग शुरू हो गई है")
        addPair("মিটিংয়ে আছি", "I am in a meeting", "मीटिंग में हूँ")
        addPair("অনলাইনে আসো", "Please come online", "ऑनलाइन आओ")
        addPair("গুগল মিটে জয়েন করো", "Join the Google Meet", "गूगल मीट जॉइन करो")
        addPair("জুমের লিংকটা পাঠাও", "Send the Zoom link", "ज़ूम का लिंक भेजो")
        addPair("স্ক্রিন শেয়ার করো", "Please share your screen", "स्क्रीन शेयर करो")
        addPair("কথা শোনা যাচ্ছে না", "You are not audible", "आवाज नहीं आ रही है")
        addPair("আমার মাইক মিউট ছিল", "My mic was muted", "मेरा माइक म्यूट था")
        addPair("ইমেলটা চেক করো", "Please check the email", "ईमेल चेक करो")
        addPair("ফাইলটা পাঠিয়ে দিয়েছি", "I have sent the file", "फाइल भेज दी है")
        addPair("হোয়াটসঅ্যাপে পাঠিয়েছি", "I sent it on WhatsApp", "व्हाट्सएप पर भेजा है")
        addPair("মেসেজটা দেখো", "Check the message", "मैसेज देखो")
        addPair("ফোন করছি", "I am calling you", "फोन कर रहा हूँ")
        addPair("একটু পর ফোন করছি", "I will call you in a bit", "थोड़ी देर में फोन करता हूँ")
        addPair("এখন কথা বলা যাবে না", "Can't talk right now", "अभी बात नहीं कर सकता")
        addPair("পরে কথা বলছি", "Will talk to you later", "बाद में बात करते हैं")
        addPair("একটু ব্যস্ত আছি", "I am a bit busy", "थोड़ा व्यस्त हूँ")
        addPair("আজকে ওয়ার্ক ফ্রম হোম", "Working from home today", "आज वर्क फ्रॉम होम है")
        addPair("কাজটা হয়ে গেছে", "The task is done", "काम हो गया है")
        addPair("কাজটা তাড়াতাড়ি শেষ করো", "Finish the work quickly", "काम जल्दी खत्म करो")

        // --- 4. Indian Digital Payments & Banking ---
        addPair("টাকা পাঠিয়ে দিয়েছি", "I have sent the money", "पैसे भेज दिए हैं")
        addPair("গুগল পে করেছি", "I have paid via Google Pay", "गूगल पे कर दिया है")
        addPair("ফোনপে চেক করো", "Check PhonePe", "फोनपे चेक करो")
        addPair("ইউপিআই আইডি দাও", "Give your UPI ID", "यूपीआई आईडी दो")
        addPair("কিউআর কোডটা স্ক্যান করো", "Scan the QR code", "क्यूआर कोड स्कैन करो")
        addPair("স্ক্রিনশট পাঠিয়ে দিয়েছি", "I have sent the screenshot", "स्क्रीनशॉट भेज दिया है")
        addPair("পেমেন্ট পেয়ে গেছি", "I have received the payment", "पेमेंट मिल गई है")
        addPair("টাকা ঢোকেনি এখনো", "Money hasn't been credited yet", "पैसे अभी तक नहीं आए")
        addPair("বিলটা পে করে দাও", "Please pay the bill", "बिल भर दो")

        // --- 5. Bengali Food & Hospitality ---
        addPair("চা খাবে?", "Would you like tea?", "चाय पियोगे?")
        addPair("এক কাপ চা খাব", "I will have a cup of tea", "एक कप चाय पियूँगा")
        addPair("দুপুরের খাবার খেয়েছ?", "Did you have lunch?", "दोपहर का खाना खा लिया?")
        addPair("রাতের খাবার খেয়েছ?", "Did you have dinner?", "रात का खाना खा लिया?")
        addPair("খুব খিদে পেয়েছে", "I am very hungry", "बहुत भूख लगी है")
        addPair("জল পিপাসা পেয়েছে", "I am thirsty", "प्यास लगी है")
        addPair("এক গ্লাস জল দাও", "Give a glass of water", "एक गिलास पानी दो")
        addPair("খাবারটা খুব সুস্বাদু", "The food is very delicious", "खाना बहुत स्वादिष्ट है")
        addPair("সুইগি থেকে অর্ডার করি", "Let's order from Swiggy", "स्वीगी से ऑर्डर करते हैं")
        addPair("জোম্যাটোতে অর্ডার দিয়েছি", "I ordered on Zomato", "ज़ोमैटो पर ऑर्डर दिया है")
        addPair("বিলটা কত হয়েছে?", "How much is the bill?", "बिल कितना हुआ?")

        // --- 6. Bengali Reassurance & Colloquialisms ---
        addPair("কোন চিন্তা নেই", "No worries at all", "कोई चिंता की बात नहीं है")
        addPair("সব ঠিক হয়ে যাবে", "Everything will be alright", "सब ठीक हो जाएगा")
        addPair("একটু সাহায্য করো", "Please help a bit", "थोड़ी मदद करो")
        addPair("একদম ঠিক কথা", "Absolutely right", "बिल्कुल सही बात है")
        addPair("আমি এটা জানতাম না", "I didn't know this", "मुझे यह नहीं पता था")
        addPair("সত্যি বলছ?", "Are you telling the truth?", "सच कह रहे हो?")
        addPair("দারুণ খবর", "Great news", "बहुत बढ़िया खबर")
        addPair("অনেক ধন্যবাদ আপনার সাহায্যের জন্য", "Thank you so much for your help", "आपकी मदद के लिए बहुत-बहुत धन्यवाद")
    }

    private fun loadHindiEnglishDataset() {
        addPair("কি খবর?", "How are you doing?", "क्या हाल है?")
        addPair("কোথায় যাচ্ছ?", "Where are you going?", "कहाँ जा रहे हो?")
        addPair("আমি বাড়িতে আছি", "I am at home", "मैं घर पर हूँ")
        addPair("তাড়াতাড়ি আসো", "Come quickly", "जल्दी आओ")
        addPair("একটু সময় লাগবে", "It will take some time", "थोड़ा समय लगेगा")
        addPair("আমাকে ফোন কোরো", "Give me a call", "मुझे फोन करना")
        addPair("আমাকে মেসেজ পাঠিও", "Send me a message", "मुझे मैसेज भेजना")
        addPair("চলো যাই", "Let's go", "चलो चलते हैं")
        addPair("সব দারুণ আছে", "Everything is great", "सब बढ़िया है")
        addPair("চিন্তা কোরো না", "Don't worry", "चिंता मत करो")
        addPair("কথা শোনো", "Listen to me", "बात सुनो")
        addPair("আমাকে বলো", "Tell me", "मुझे बताओ")
        addPair("আমি বুঝতে পারিনি", "I didn't understand", "मुझे समझ नहीं आया")
        addPair("দয়া করে আবার বলুন", "Please repeat", "कृपया दोहराएं")
    }

    private fun loadBengaliHindiDataset() {
        addPair("কোথায় যাবে?", "Where will you go?", "कहाँ जाओगे?")
        addPair("আমার সাথে আসো", "Come with me", "मेरे साथ आओ")
        addPair("এখানে বসো", "Sit here", "यहाँ बैठो")
        addPair("তাড়াতাড়ি করো", "Hurry up", "जल्दी करो")
    }
}
