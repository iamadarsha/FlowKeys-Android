package com.flowkeys.android.dictionary

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Local custom vocabulary and Indian proper noun replacement engine.
 * Automatically integrates user contacts and common Indian terminology.
 */
object PersonalDictionary {

    private val commonIndianVocabulary = mapOf(
        "(?i)\\braul\\b" to "Rahul",
        "(?i)\\brohan\\b" to "Rohan",
        "(?i)\\bpriya\\b" to "Priya",
        "(?i)\\bananya\\b" to "Ananya",
        "(?i)\\barjun\\b" to "Arjun",
        "(?i)\\bsubhashish\\b" to "Subhashish",
        "(?i)\\bdebolina\\b" to "Debolina",
        "(?i)\\bsourav\\b" to "Sourav",
        "(?i)\\badarsha\\b" to "Adarsha",
        "(?i)\\bkolkotta\\b" to "Kolkata",
        "(?i)\\bcalcutta\\b" to "Kolkata",
        "(?i)\\bbengaluru\\b" to "Bengaluru",
        "(?i)\\bbangalore\\b" to "Bengaluru",
        "(?i)\\bmumbai\\b" to "Mumbai",
        "(?i)\\bdelhi\\b" to "Delhi",
        "(?i)\\bchennai\\b" to "Chennai",
        "(?i)\\bhyderabad\\b" to "Hyderabad",
        "(?i)\\bflipkart\\b" to "Flipkart",
        "(?i)\\bswiggy\\b" to "Swiggy",
        "(?i)\\bzomato\\b" to "Zomato",
        "(?i)\\bzerodha\\b" to "Zerodha",
        "(?i)\\bpaytm\\b" to "Paytm",
        "(?i)\\bphonepe\\b" to "PhonePe",
        "(?i)\\bgpay\\b" to "GPay",
        "(?i)\\bupi\\b" to "UPI",
        "(?i)\\baadhaar\\b" to "Aadhaar",
        "(?i)\\bwhatsapp\\b" to "WhatsApp",
        "(?i)\\binstagram\\b" to "Instagram",
        "(?i)\\btelegram\\b" to "Telegram"
    )

    private val userCustomWords = ConcurrentHashMap<String, String>()
    private val learnedContactNames = CopyOnWriteArraySet<String>()

    fun applyVocabulary(text: String): String {
        if (text.isBlank()) return text
        var result = text

        // 1. Built-in vocabulary & brand normalizations
        for ((pattern, replacement) in commonIndianVocabulary) {
            result = result.replace(Regex(pattern), replacement)
        }

        // 2. Learned contact names (case-insensitive boundary match to preserve contact casing)
        for (contact in learnedContactNames) {
            if (contact.length >= 3) {
                result = result.replace(Regex("(?i)\\b${Regex.escape(contact)}\\b"), contact)
            }
        }

        // 3. User custom words & snippets
        for ((pattern, replacement) in userCustomWords) {
            result = result.replace(Regex("(?i)\\b${Regex.escape(pattern)}\\b"), replacement)
        }

        return result
    }

    fun addContactName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            learnedContactNames.add(trimmed)
        }
    }

    fun addWord(spoken: String, replacement: String) {
        userCustomWords[spoken.trim()] = replacement.trim()
    }

    fun getLearnedContactCount(): Int = learnedContactNames.size

    fun clearContacts() {
        learnedContactNames.clear()
    }
}
