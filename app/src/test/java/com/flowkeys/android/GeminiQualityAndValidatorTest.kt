package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.validation.OutputValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiQualityAndValidatorTest {

    @Test
    fun testOutputValidatorAcceptsValidBengaliToEnglish() {
        val source = "কাল আমাকে অফিস থেকে একটু আগে বের হতে হবে কারণ আমার ডাক্তার দেখানোর অ্যাপয়েন্টমেন্ট আছে।"
        val candidate = "I need to leave the office a little early tomorrow because I have a doctor's appointment."
        val result = OutputValidator.validate(candidate, source, Language.ENGLISH, isTranslation = true)
        assertTrue(result.isValid)
    }

    @Test
    fun testOutputValidatorAcceptsValidBengaliToHindi() {
        val source = "কাল আমাকে অফিস থেকে একটু আগে বের হতে হবে কারণ আমার ডাক্তার দেখানোর অ্যাপয়েন্টমেন্ট আছে।"
        val candidate = "मुझे कल ऑफिस से थोड़ा जल्दी निकलना होगा क्योंकि मेरे डॉक्टर से मिलने का अपॉइंटमेंट है।"
        val result = OutputValidator.validate(candidate, source, Language.HINDI, isTranslation = true)
        assertTrue(result.isValid)
    }

    @Test
    fun testOutputValidatorRejectsPreamble() {
        val source = "কালকে আসব"
        val candidate = "Here is the translation: I will come tomorrow."
        val result = OutputValidator.validate(candidate, source, Language.ENGLISH, isTranslation = true)
        assertFalse(result.isValid)
    }

    @Test
    fun testOutputValidatorRejectsRunawayBalloonText() {
        val source = "কেমন আছেন"
        val candidate = "Hello, I am asking you how you are doing today in this wonderful morning, hoping everything is great with you and your family."
        val result = OutputValidator.validate(candidate, source, Language.ENGLISH, isTranslation = true)
        assertFalse(result.isValid)
    }

    @Test
    fun testOutputValidatorRejectsWrongScriptForBengali() {
        val source = "Where are you going?"
        // Candidate returned English instead of Bengali
        val candidate = "Where are you heading right now?"
        val result = OutputValidator.validate(candidate, source, Language.BENGALI, isTranslation = true)
        assertFalse(result.isValid)
    }

    @Test
    fun testOutputValidatorPreservesCodeSwitchingBengaliEnglish() {
        val source = "কাল আমাকে ওই API-র responseটা check করতে হবে।"
        val candidate = "Tomorrow I need to check the response of that API."
        val result = OutputValidator.validate(candidate, source, Language.ENGLISH, isTranslation = true)
        assertTrue(result.isValid)
    }

    @Test
    fun testOutputValidatorPreservesHinglishWithNumbers() {
        val source = "kal mujhe office jaana hai but meeting 4 baje hai"
        val candidate = "I need to go to the office tomorrow, but the meeting is at 4 PM."
        val result = OutputValidator.validate(candidate, source, Language.ENGLISH, isTranslation = true)
        assertTrue(result.isValid)
    }
}
