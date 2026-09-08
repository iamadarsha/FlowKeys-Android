package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.dictionary.IndicSpeechLexicon2026
import com.flowkeys.android.dictionary.IndicSpeechLexicon2026.LexiconTier
import com.flowkeys.android.polish.MicroPolisher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IndicSpeechLexicon2026Test {

    @Before
    fun setUp() {
        IndicSpeechLexicon2026.initializeForTesting(LexiconTier.EXTENDED_6GB_PLUS)
    }

    @Test
    fun testCompact4GbTierLimitsMemoryAndEntries() {
        IndicSpeechLexicon2026.initializeForTesting(LexiconTier.COMPACT_4GB)

        assertEquals(LexiconTier.COMPACT_4GB, IndicSpeechLexicon2026.getTier())
        val count = IndicSpeechLexicon2026.getTotalEntryCount()
        assertTrue("Compact tier should be lean (< 100 entries) for zero 4GB RAM pressure, was $count", count in 40..95)

        // Core 2026 brands still disambiguated safely
        val result = IndicSpeechLexicon2026.disambiguate("জেপ্টো এবং ব্লিনকিট থেকে অর্ডার করো", Language.BENGALI)
        assertTrue("Should contain Zepto", result.contains("Zepto"))
        assertTrue("Should contain Blinkit", result.contains("Blinkit"))
    }

    @Test
    fun testExtended6GbPlusTierLoadsFullLexicon() {
        IndicSpeechLexicon2026.initializeForTesting(LexiconTier.EXTENDED_6GB_PLUS)

        assertEquals(LexiconTier.EXTENDED_6GB_PLUS, IndicSpeechLexicon2026.getTier())
        val count = IndicSpeechLexicon2026.getTotalEntryCount()
        assertTrue("Extended tier should contain > 100 entries, was $count", count > 100)

        // Extended 2026 Bengali urban transit & code-mixed tech
        val resultBn = IndicSpeechLexicon2026.disambiguate("আমি উবার নিয়ে সল্টলেক যাচ্ছি পিআর দেখতে", Language.BENGALI)
        assertTrue("Should resolve Uber", resultBn.contains("Uber"))
        assertTrue("Should resolve সল্টলেক", resultBn.contains("সল্টলেক"))
        assertTrue("Should resolve PR", resultBn.contains("PR"))

        // Extended 2026 Hindi urban transit & code-mixed tech
        val resultHi = IndicSpeechLexicon2026.disambiguate("मैं उबर से गुड़गांव जा रहा हूँ पीआर चेक करने", Language.HINDI)
        assertTrue("Should resolve Uber", resultHi.contains("Uber"))
        assertTrue("Should resolve गुरुग्राम", resultHi.contains("गुरुग्राम"))
        assertTrue("Should resolve PR", resultHi.contains("PR"))
    }

    @Test
    fun testVocabHintsBiasing() {
        val bnHints = IndicSpeechLexicon2026.getVocabHints(Language.BENGALI, limit = 16)
        assertTrue("Bengali hints should contain Zepto", bnHints.contains("Zepto"))
        assertTrue("Bengali hints should contain Blinkit", bnHints.contains("Blinkit"))
        assertTrue("Bengali hints should contain Gemini", bnHints.contains("Gemini"))
        assertTrue("Bengali hints should contain ChatGPT", bnHints.contains("ChatGPT"))
        assertTrue("Bengali hints should contain WhatsApp", bnHints.contains("WhatsApp"))

        val hiHints = IndicSpeechLexicon2026.getVocabHints(Language.HINDI, limit = 16)
        assertTrue("Hindi hints should contain Zepto", hiHints.contains("Zepto"))
        assertTrue("Hindi hints should contain Blinkit", hiHints.contains("Blinkit"))
        assertTrue("Hindi hints should contain UPI", hiHints.contains("UPI"))
    }

    @Test
    fun testMicroPolisherIntegrationWithIndicLexicon() {
        IndicSpeechLexicon2026.initializeForTesting(LexiconTier.EXTENDED_6GB_PLUS)

        // Bengali disambiguation via MicroPolisher
        val bnPolished = MicroPolisher.polish("জেপ্টো থেকে খাবার অর্ডার করো", Language.BENGALI)
        assertTrue("MicroPolisher should resolve জেপ্টো -> Zepto, was: $bnPolished", bnPolished.contains("Zepto"))

        // Hindi disambiguation via MicroPolisher
        val hiPolished = MicroPolisher.polish("ज़ेप्टो से ग्रोसरी मंगवाओ", Language.HINDI)
        assertTrue("MicroPolisher should resolve ज़ेप्टो -> Zepto, was: $hiPolished", hiPolished.contains("Zepto"))
    }
}
