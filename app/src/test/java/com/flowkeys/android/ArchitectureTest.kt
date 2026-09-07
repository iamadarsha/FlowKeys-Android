package com.flowkeys.android

import com.flowkeys.android.core.model.DeviceTier
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.modes.AppAwareDetector
import com.flowkeys.android.modes.SmartMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureTest {

    @Test
    fun testDeviceTierConstraints() {
        val tier1 = DeviceTier.TIER_1_4GB
        assertEquals("4 GB tier should strictly use at most 2 threads", 2, tier1.maxThreads)
        assertFalse("4 GB tier must strictly disallow local LLM", tier1.allowLocalLlm)
        assertFalse("4 GB tier must disallow speculative warmup", tier1.speculativeWarmupAllowed)

        val tier3 = DeviceTier.TIER_3_8GB_PLUS
        assertEquals("8 GB+ tier can utilize 4 threads", 4, tier3.maxThreads)
        assertTrue("8 GB+ tier allows optional local LLM", tier3.allowLocalLlm)
        assertTrue("8 GB+ tier allows speculative warmup", tier3.speculativeWarmupAllowed)
    }

    @Test
    fun testAppAwareModeDetection() {
        assertEquals(SmartMode.CHAT, AppAwareDetector.detectMode("com.whatsapp"))
        assertEquals(SmartMode.PROFESSIONAL, AppAwareDetector.detectMode("com.google.android.gm"))
        assertEquals(SmartMode.DEVELOPER, AppAwareDetector.detectMode("com.termux"))
        assertEquals(SmartMode.GENERAL, AppAwareDetector.detectMode("com.random.notes"))
    }

    @Test
    fun testLanguageCodeMapping() {
        assertEquals(Language.BENGALI, Language.fromCode("bn"))
        assertEquals(Language.HINDI, Language.fromCode("hi"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.DEFAULT, Language.fromCode("unknown"))
    }
}
