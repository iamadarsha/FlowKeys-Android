package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.dictionary.PersonalDictionary
import com.flowkeys.android.polish.MicroPolisher
import com.flowkeys.android.polish.bengali.BengaliPolisher
import com.flowkeys.android.polish.correction.CorrectionResolver
import com.flowkeys.android.polish.english.EnglishPolisher
import com.flowkeys.android.polish.hindi.HindiPolisher
import com.flowkeys.android.polish.normalizers.IndianNumberNormalizer
import com.flowkeys.android.polish.normalizers.ScriptValidator
import com.flowkeys.android.snippets.SnippetEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroPolisherTest {

    @Test
    fun testBengaliWestBengalColloquialFillerRemoval() {
        val input = "কালকে আসলে মিটিং আছে"
        val expected = "কালকে মিটিং আছে।"
        val output = BengaliPolisher.polish(input)
        assertEquals(expected, output)
    }

    @Test
    fun testBengaliPhonemicLoanwordMapping() {
        val input = "কালকে office e jabo na"
        val output = BengaliPolisher.polish(input)
        assertTrue("Output should contain অফিস", output.contains("অফিস"))
    }

    @Test
    fun testBengaliQuestionMarkInjection() {
        val input = "তুমি কখন আসবে"
        val output = BengaliPolisher.polish(input)
        assertTrue("Interrogative sentence should end with ?", output.endsWith("?"))
    }

    @Test
    fun testHindiFillerRemovalAndPunctuation() {
        val input = "मुझे मतलब कल ऑफिस जाना है"
        val expected = "मुझे कल ऑफिस जाना है।"
        val output = HindiPolisher.polish(input)
        assertEquals(expected, output)
    }

    @Test
    fun testHindiQuestionMarkInjection() {
        val input = "आप कब आएंगे"
        val output = HindiPolisher.polish(input)
        assertTrue("Interrogative Hindi sentence should end with ?", output.endsWith("?"))
    }

    @Test
    fun testEnglishFillerRemovalAndCapitalization() {
        val input = "um i wanted to check the report"
        val expected = "I wanted to check the report."
        val output = EnglishPolisher.polish(input)
        assertEquals(expected, output)
    }

    @Test
    fun testIndianCurrencyFormatting() {
        val input = "The price is 50000 rupees"
        val output = IndianNumberNormalizer.normalize(input, Language.ENGLISH)
        assertEquals("The price is ₹50,000", output)
    }

    @Test
    fun testIndianGroupingLakhs() {
        val input = "25 lakh"
        val output = IndianNumberNormalizer.normalize(input, Language.ENGLISH)
        assertEquals("25,00,000", output)
    }

    @Test
    fun testConversationalSelfCorrection() {
        val input = "Let's meet at 4 PM sorry 5 PM"
        val output = CorrectionResolver.resolve(input, Language.ENGLISH)
        assertEquals("5 PM", output)
    }

    @Test
    fun testScriptValidator() {
        val bengaliText = "কালকে মিটিং আছে"
        assertTrue("Bengali text must validate for Language.BENGALI", ScriptValidator.isValidScript(bengaliText, Language.BENGALI))

        val hindiText = "कल मीटिंग है"
        assertTrue("Hindi text must validate for Language.HINDI", ScriptValidator.isValidScript(hindiText, Language.HINDI))

        val englishText = "The meeting is tomorrow"
        assertTrue("English text must validate for Language.ENGLISH", ScriptValidator.isValidScript(englishText, Language.ENGLISH))
    }

    @Test
    fun testPersonalDictionary() {
        PersonalDictionary.addWord("flowkey", "FlowKeys")
        val input = "I am using flowkey dictation"
        val output = PersonalDictionary.applyVocabulary(input)
        assertTrue(output.contains("FlowKeys"))
    }

    @Test
    fun testSnippetExpansion() {
        SnippetEngine.registerSnippet("my promo", "Use code FLOWKEYS2026 for 100% free access")
        val input = "Please enter my promo"
        val output = SnippetEngine.expand(input)
        assertTrue(output.contains("Use code FLOWKEYS2026"))
    }

    @Test
    fun testEndToEndMicroPolisher() {
        val rawInput = "um kal मुझे office जाना है"
        val polished = MicroPolisher.polish(rawInput, Language.HINDI)
        assertTrue("Should strip 'um'", !polished.contains("um"))
        assertTrue("Should preserve Hindi Devanagari text", polished.contains("जाना है"))
    }

    @Test
    fun testBengaliDativeWordsDoNotTriggerQuestion() {
        val input = "আমাকে কালকে রামকে জানাতে হবে"
        val output = BengaliPolisher.polish(input)
        assertTrue("Sentence ending in -কে words must end with Dari, not question mark", output.endsWith("।"))
    }

    @Test
    fun testBengaliMultipleFillersRemoval() {
        val input = "আসলে আমি ওই যে মিটিংয়ে যাব"
        val output = BengaliPolisher.polish(input)
        assertTrue("Should strip multiple fillers", !output.contains("আসলে") && !output.contains("ওই যে"))
        assertTrue("Should retain core sentence", output.contains("আমি") && output.contains("মিটিং"))
    }

    @Test
    fun testEmptyAndBlankInputs() {
        assertEquals("", MicroPolisher.polish("", Language.BENGALI))
        assertEquals("", MicroPolisher.polish("   ", Language.HINDI))
        assertEquals("", MicroPolisher.polish("", Language.ENGLISH))
    }
}
