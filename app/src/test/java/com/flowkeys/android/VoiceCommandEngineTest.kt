package com.flowkeys.android

import com.flowkeys.android.core.model.DictationContext
import com.flowkeys.android.core.model.FieldType
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.commands.VoiceCommandEngine
import com.flowkeys.android.polish.context.ContextAwareFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandEngineTest {

    @Test
    fun testEnglishVoicePunctuationAndStructure() {
        val input = "Hello comma please send the file new line thanks full stop"
        val output = VoiceCommandEngine.process(input, Language.ENGLISH)
        assertTrue(output.contains("Hello,"))
        assertTrue(output.contains("\n"))
        assertTrue(output.contains("thanks."))
    }

    @Test
    fun testEnglishBulletPointAndQuotes() {
        val input = "Items bullet point task one bullet point task two quote important end quote"
        val output = VoiceCommandEngine.process(input, Language.ENGLISH)
        assertTrue(output.contains("• task one"))
        assertTrue(output.contains("• task two"))
        assertTrue(output.contains("\"important\""))
    }

    @Test
    fun testEnglishScratchThat() {
        val input = "We will meet at five PM scratch that six PM"
        val output = VoiceCommandEngine.process(input, Language.ENGLISH)
        assertEquals("six PM", output)
    }

    @Test
    fun testBengaliVoicePunctuation() {
        val input = "কাল এসো কমা দশটায় মিটিং দাঁড়ি"
        val output = VoiceCommandEngine.process(input, Language.BENGALI)
        assertTrue(output.contains("কাল এসো,"))
        assertTrue(output.contains("মিটিং।"))
    }

    @Test
    fun testBengaliNewLineAndBullet() {
        val input = "কাজের তালিকা নতুন লাইন বুলেট পয়েন্ট প্রথম কাজ পরের লাইন বুলেট পয়েন্ট দ্বিতীয় কাজ"
        val output = VoiceCommandEngine.process(input, Language.BENGALI)
        assertTrue(output.contains("• প্রথম কাজ"))
        assertTrue(output.contains("• দ্বিতীয় কাজ"))
    }

    @Test
    fun testHindiVoicePunctuationAndLines() {
        val input = "नमस्ते कॉमा कल मिलते हैं पूर्ण विराम नई लाइन बुलेट पॉइंट पहला काम"
        val output = VoiceCommandEngine.process(input, Language.HINDI)
        assertTrue(output.contains("नमस्ते,"))
        assertTrue(output.contains("मिलते हैं।"))
        assertTrue(output.contains("• पहला काम"))
    }

    @Test
    fun testContextAwareSearchBarStripsPunctuation() {
        val searchContext = DictationContext(
            packageName = "com.android.chrome",
            fieldType = FieldType.SEARCH_BAR,
            language = Language.ENGLISH
        )
        val input = "How to install FlowKeys on Android."
        val output = ContextAwareFormatter.format(input, Language.ENGLISH, searchContext)
        assertEquals("How to install FlowKeys on Android", output)
    }

    @Test
    fun testContextAwareDeveloperTerminalPreservesCode() {
        val devContext = DictationContext(
            packageName = "com.termux",
            fieldType = FieldType.DEVELOPER_TERMINAL,
            language = Language.ENGLISH
        )
        val input = "git checkout -b feature/flowkeys_v1"
        val output = ContextAwareFormatter.format(input, Language.ENGLISH, devContext)
        assertEquals("git checkout -b feature/flowkeys_v1", output)
    }

    @Test
    fun testContextAwareEmailSalutationFormatting() {
        val emailContext = DictationContext(
            packageName = "com.google.android.gm",
            fieldType = FieldType.EMAIL,
            language = Language.ENGLISH
        )
        val input = "hi team, the report is ready."
        val output = ContextAwareFormatter.format(input, Language.ENGLISH, emailContext)
        assertTrue(output.startsWith("Hi team,"))
    }

    @Test
    fun testStartAgainBacktracks() {
        val enInput = "Send draft to John start again send draft to Sarah"
        val enOutput = VoiceCommandEngine.process(enInput, Language.ENGLISH)
        assertEquals("send draft to Sarah", enOutput)

        val bnInput = "কাল যাব নতুন করে বলছি পরশু যাব"
        val bnOutput = VoiceCommandEngine.process(bnInput, Language.BENGALI)
        assertEquals("পরশু যাব", bnOutput)

        val hiInput = "कल आऊंगा फिर से बोल रहा हूँ परसों आऊंगा"
        val hiOutput = VoiceCommandEngine.process(hiInput, Language.HINDI)
        assertEquals("परसों आऊंगा", hiOutput)
    }

    @Test
    fun testSmartModeRawPreservesVerbatimInput() {
        val rawInput = "ummm actually yeah https://github.com/flowkeys"
        val output = com.flowkeys.android.polish.MicroPolisher.polish(
            rawText = rawInput,
            language = Language.ENGLISH,
            smartMode = com.flowkeys.android.modes.SmartMode.RAW
        )
        assertEquals(rawInput, output)
    }
}
