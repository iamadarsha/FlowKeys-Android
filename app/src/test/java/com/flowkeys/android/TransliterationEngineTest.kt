package com.flowkeys.android

import com.flowkeys.android.core.model.ScriptMode
import com.flowkeys.android.dictionary.PersonalDictionary
import com.flowkeys.android.polish.transliteration.BengaliLatinTransliterator
import com.flowkeys.android.polish.transliteration.HindiLatinTransliterator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransliterationEngineTest {

    @Test
    fun testBengaliToLatinBenglishBasic() {
        val input = "আমি কাল অফিসে যাব"
        val output = BengaliLatinTransliterator.transliterate(input)
        assertTrue("Output should start with Ami: $output", output.startsWith("Ami"))
        assertTrue("Output should contain kal: $output", output.contains("kal", ignoreCase = true))
        assertTrue("Output should contain office: $output", output.contains("office", ignoreCase = true))
    }

    @Test
    fun testBengaliToLatinBenglishQuestion() {
        val input = "তুমি কেমন আছো?"
        val output = BengaliLatinTransliterator.transliterate(input)
        assertEquals("Tumi kemon achho?", output)
    }

    @Test
    fun testBengaliToLatinCommonGreetings() {
        val input = "সবাইকে ধন্যবাদ"
        val output = BengaliLatinTransliterator.transliterate(input)
        assertTrue("Output should contain dhonnobad: $output", output.contains("dhonnobad", ignoreCase = true))
    }

    @Test
    fun testHindiToLatinHinglishBasic() {
        val input = "मैं कल ऑफिस जा रहा हूँ"
        val output = HindiLatinTransliterator.transliterate(input)
        assertEquals("Main kal office ja raha hoon.", output)
    }

    @Test
    fun testHindiToLatinHinglishQuestion() {
        val input = "आप कैसे हैं?"
        val output = HindiLatinTransliterator.transliterate(input)
        assertEquals("Aap kaise hain?", output)
    }

    @Test
    fun testHindiToLatinConversational() {
        val input = "हम अभी घर पहुँच रहे हैं"
        val output = HindiLatinTransliterator.transliterate(input)
        assertTrue("Output should contain hum: $output", output.startsWith("Hum"))
        assertTrue("Output should contain abhi: $output", output.contains("abhi"))
        assertTrue("Output should contain ghar: $output", output.contains("ghar"))
    }

    @Test
    fun testPersonalDictionaryContactLearning() {
        PersonalDictionary.addContactName("Subhashish Banerjee")
        PersonalDictionary.addContactName("Debolina")
        PersonalDictionary.addContactName("Ananya")

        val input = "call subhashish banerjee and message debolina"
        val output = PersonalDictionary.applyVocabulary(input)
        assertTrue("Subhashish Banerjee should be capitalized: $output", output.contains("Subhashish Banerjee"))
        assertTrue("Debolina should be capitalized: $output", output.contains("Debolina"))
    }

    @Test
    fun testScriptModeEnum() {
        assertEquals(ScriptMode.TRANSLATED_ENGLISH, ScriptMode.fromId("translated_en"))
        assertEquals(ScriptMode.NATIVE_SCRIPT, ScriptMode.fromId("native_script"))
        assertEquals(ScriptMode.PHONETIC_LATIN, ScriptMode.fromId("phonetic_latin"))
        assertEquals(ScriptMode.DEFAULT, ScriptMode.fromId("invalid_id"))
    }
}
