package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.polish.MicroPolisher
import com.flowkeys.android.polish.spans.ProtectedSpanManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedSpanManagerTest {

    @Test
    fun testUrlPreservation() {
        val input = "Check out https://github.com/flowkeys for details"
        val masked = ProtectedSpanManager.mask(input)
        assertTrue(masked.placeholderMap.values.contains("https://github.com/flowkeys"))
        val unmasked = ProtectedSpanManager.unmask(masked.maskedText, masked.placeholderMap)
        assertEquals(input, unmasked)
    }

    @Test
    fun testEmailPreservation() {
        val input = "Send an email to team@flowkeys.ai please"
        val masked = ProtectedSpanManager.mask(input)
        assertTrue(masked.placeholderMap.values.contains("team@flowkeys.ai"))
        val unmasked = ProtectedSpanManager.unmask(masked.maskedText, masked.placeholderMap)
        assertEquals(input, unmasked)
    }

    @Test
    fun testPhoneNumberPreservation() {
        val input = "Call me at +91 9876543210 tomorrow"
        val masked = ProtectedSpanManager.mask(input)
        assertTrue(masked.placeholderMap.values.any { it.contains("9876543210") })
        val unmasked = ProtectedSpanManager.unmask(masked.maskedText, masked.placeholderMap)
        assertEquals(input, unmasked)
    }

    @Test
    fun testCurrencyPreservation() {
        val input = "The bill is ₹5,000 for the dinner"
        val masked = ProtectedSpanManager.mask(input)
        assertTrue(masked.placeholderMap.values.any { it.contains("₹5,000") })
        val unmasked = ProtectedSpanManager.unmask(masked.maskedText, masked.placeholderMap)
        assertEquals(input, unmasked)
    }

    @Test
    fun testCodeIdentifierPreservation() {
        val input = "Please inspect user_account_id and fetchUserData"
        val masked = ProtectedSpanManager.mask(input)
        assertTrue(masked.placeholderMap.values.contains("user_account_id"))
        assertTrue(masked.placeholderMap.values.contains("fetchUserData"))
        val unmasked = ProtectedSpanManager.unmask(masked.maskedText, masked.placeholderMap)
        assertEquals(input, unmasked)
    }

    @Test
    fun testMicroPolisherPreservesTechnicalSpansInBengali() {
        val raw = "তুমি কি https://flowkeys.org দেখেছো"
        val polished = MicroPolisher.polish(raw, Language.BENGALI)
        assertTrue(polished.contains("https://flowkeys.org"))
    }

    @Test
    fun testMicroPolisherPreservesEmailInHindi() {
        val raw = "मेरा ईमेल support@flowkeys.io है"
        val polished = MicroPolisher.polish(raw, Language.HINDI)
        assertTrue(polished.contains("support@flowkeys.io"))
    }
}
