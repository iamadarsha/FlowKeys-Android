package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.translation.TranslationEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationEngineTest {

    @Test
    fun testBengaliToEnglishTranslation() = runBlocking {
        val input = "কাল আমাকে অফিসে যেতে হবে"
        val translated = TranslationEngine.translate(input, Language.BENGALI, Language.ENGLISH)
        assertEquals("I have to go to the office tomorrow.", translated)
    }

    @Test
    fun testBengaliBenglishToEnglish() = runBlocking {
        val input = "আমি office যাচ্ছি"
        val translated = TranslationEngine.translate(input, Language.BENGALI, Language.ENGLISH)
        assertEquals("I am going to office.", translated)
    }

    @Test
    fun testBengaliWhatsAppMessageToEnglish() = runBlocking {
        val input = "হোয়াটসঅ্যাপে মেসেজ করো"
        val translated = TranslationEngine.translate(input, Language.BENGALI, Language.ENGLISH)
        assertEquals("Message me on WhatsApp.", translated)
    }

    @Test
    fun testBengaliLateWarningToEnglish() = runBlocking {
        val input = "দেরি হয়ে যাবে"
        val translated = TranslationEngine.translate(input, Language.BENGALI, Language.ENGLISH)
        assertEquals("I will be late.", translated)
    }

    @Test
    fun testHindiToEnglishTranslation() = runBlocking {
        val input = "मुझे कल ऑफिस जाना है"
        val translated = TranslationEngine.translate(input, Language.HINDI, Language.ENGLISH)
        assertEquals("I have to go to the office tomorrow.", translated)
    }

    @Test
    fun testHindiHinglishToEnglish() = runBlocking {
        val input = "मैं office जा रहा हूँ"
        val translated = TranslationEngine.translate(input, Language.HINDI, Language.ENGLISH)
        assertEquals("I am going to office.", translated)
    }

    @Test
    fun testEnglishToBengaliTranslation() = runBlocking {
        val input = "how are you"
        val translated = TranslationEngine.translate(input, Language.ENGLISH, Language.BENGALI)
        assertTrue(translated.contains("কেমন আছেন"))
    }

    @Test
    fun testEnglishToHindiTranslation() = runBlocking {
        val input = "how are you"
        val translated = TranslationEngine.translate(input, Language.ENGLISH, Language.HINDI)
        assertTrue(translated.contains("कैसे हैं"))
    }

    @Test
    fun testBengaliToHindiTranslation() = runBlocking {
        val input = "দাদা একটু জল পাব?"
        val translated = TranslationEngine.translate(input, Language.BENGALI, Language.HINDI)
        assertTrue(translated.contains("पानी"))
    }

    @Test
    fun testSameLanguageIdentity() = runBlocking {
        val input = "আমার নাম আদর্শ"
        val result = TranslationEngine.translate(input, Language.BENGALI, Language.BENGALI)
        assertEquals(input, result)
    }

    @Test
    fun testBengaliGreetingsToEnglish() = runBlocking {
        val input = "নমস্কার"
        val result = TranslationEngine.translate(input, Language.BENGALI, Language.ENGLISH)
        assertEquals("Hello.", result)
    }

    @Test
    fun testHindiToBengaliCommute() = runBlocking {
        val input = "मैं रास्ते में हूँ"
        val result = TranslationEngine.translate(input, Language.HINDI, Language.BENGALI)
        assertTrue("Should translate to Bengali on-my-way", result.contains("রাস্তায় আছি") || result.contains("রাস্তায় আছি"))
    }

    @Test
    fun testEnglishToHindiWork() = runBlocking {
        val input = "i have sent the file"
        val result = TranslationEngine.translate(input, Language.ENGLISH, Language.HINDI)
        assertTrue("Should translate to Hindi file sent", result.contains("फ़ाइल") || result.contains("भेज"))
    }

    @Test
    fun testBengaliToHindiAllGood() = runBlocking {
        val input = "সব ঠিক আছে"
        val result = TranslationEngine.translate(input, Language.BENGALI, Language.HINDI)
        assertEquals("सब ठीक है।", result)
    }

    @Test
    fun testBlankAndWhitespaceInput() = runBlocking {
        val blank = "   "
        val result = TranslationEngine.translate(blank, Language.BENGALI, Language.HINDI)
        assertEquals(blank, result)
    }
}
