package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.translation.OfflineIndicLexicon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineIndicLexiconTest {

    @Test
    fun testKolkataBengaliPhrasesLookup() {
        val result1 = OfflineIndicLexicon.lookup("অফিসে পৌঁছে গেছি", Language.BENGALI, Language.ENGLISH)
        assertEquals("Arrived at office", result1)

        val result2 = OfflineIndicLexicon.lookup("মেট্রো ধরে নিয়েছি", Language.BENGALI, Language.ENGLISH)
        assertEquals("I have boarded the metro", result2)

        val result3 = OfflineIndicLexicon.lookup("গুগল পে করেছি", Language.BENGALI, Language.ENGLISH)
        assertEquals("I have paid via Google Pay", result3)

        val result4 = OfflineIndicLexicon.lookup("চা খাবে?", Language.BENGALI, Language.ENGLISH)
        assertEquals("Would you like tea?", result4)

        val result5 = OfflineIndicLexicon.lookup("রাস্তায় খুব জ্যাম", Language.BENGALI, Language.ENGLISH)
        assertEquals("Traffic is jammed", result5)
    }

    @Test
    fun testHindiToEnglishLookup() {
        val result = OfflineIndicLexicon.lookup("क्या हाल है?", Language.HINDI, Language.ENGLISH)
        assertEquals("How are you doing?", result)

        val result2 = OfflineIndicLexicon.lookup("मैं घर पर हूँ", Language.HINDI, Language.ENGLISH)
        assertEquals("I am at home", result2)
    }

    @Test
    fun testEnglishToBengaliLookup() {
        val result = OfflineIndicLexicon.lookup("Good morning", Language.ENGLISH, Language.BENGALI)
        assertEquals("সুপ্রভাত", result)

        val result2 = OfflineIndicLexicon.lookup("I am on the way", Language.ENGLISH, Language.BENGALI)
        assertEquals("আমি রাস্তায় আছি", result2)
    }

    @Test
    fun testInternetAndWhatsAppSlangExpansion() {
        val input1 = "I am wfh today please msg asap"
        val output1 = OfflineIndicLexicon.expandInternetSlang(input1)
        assertEquals("I am working from home today please msg as soon as possible", output1)

        val input2 = "Please revert back and do the needful"
        val output2 = OfflineIndicLexicon.expandInternetSlang(input2)
        assertEquals("Please reply and take the necessary action", output2)

        val input3 = "Can we prepone the meeting"
        val output3 = OfflineIndicLexicon.expandInternetSlang(input3)
        assertEquals("Can we reschedule earlier the meeting", output3)

        val input4 = "He is my cousin brother"
        val output4 = OfflineIndicLexicon.expandInternetSlang(input4)
        assertEquals("He is my cousin", output4)
    }

    @Test
    fun testWestBengalColloquialNormalization() {
        val input1 = "Ami office jachhi"
        val output1 = OfflineIndicLexicon.normalizeWestBengalBengali(input1)
        assertTrue("Should normalize office jachhi to অফিসে যাচ্ছি", output1.contains("অফিসে যাচ্ছি"))

        val input2 = "Ami meeting e achhi"
        val output2 = OfflineIndicLexicon.normalizeWestBengalBengali(input2)
        assertTrue("Should normalize meeting e achhi to মিটিংয়ে আছি", output2.contains("মিটিংয়ে আছি"))

        val input3 = "Taka gpay korechi"
        val output3 = OfflineIndicLexicon.normalizeWestBengalBengali(input3)
        assertTrue("Should normalize gpay korechi to গুগল পে করেছি", output3.contains("গুগল পে করেছি"))
    }

    @Test
    fun testSubMillisecondTriePerformance() {
        val start = System.nanoTime()
        for (i in 0 until 1000) {
            val res = OfflineIndicLexicon.lookup("অফিসে পৌঁছে গেছি", Language.BENGALI, Language.ENGLISH)
            assertNotNull(res)
        }
        val durationNs = System.nanoTime() - start
        val avgDurationUs = (durationNs / 1000) / 1000.0 // average in microseconds
        assertTrue("Average lookup should be sub-millisecond (was ${avgDurationUs}us)", avgDurationUs < 1000.0)
    }
}
