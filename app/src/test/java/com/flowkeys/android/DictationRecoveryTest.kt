package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.history.DictationHistoryRepository
import com.flowkeys.android.recovery.DictationRecoveryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DictationRecoveryTest {

    @Before
    fun setup() {
        DictationRecoveryManager.clear()
        DictationHistoryRepository.clear()
    }

    @Test
    fun testDictationRecoveryManagerStoresAndRetrievesFailedText() {
        assertNull(DictationRecoveryManager.getLatestRecoverable())

        val text = "Important meeting notes about project release"
        DictationRecoveryManager.saveRecoverable(
            text = text,
            language = Language.ENGLISH,
            packageName = "com.whatsapp",
            reason = "Node detached before commit"
        )

        val recovered = DictationRecoveryManager.getLatestRecoverable()
        assertNotNull(recovered)
        assertEquals(text, recovered?.text)
        assertEquals(Language.ENGLISH, recovered?.language)
        assertEquals("com.whatsapp", recovered?.packageName)

        DictationRecoveryManager.clear()
        assertNull(DictationRecoveryManager.getLatestRecoverable())
    }

    @Test
    fun testDictationHistoryRepositoryTracksRecentDictations() {
        assertEquals(0, DictationHistoryRepository.historyFlow.value.size)

        DictationHistoryRepository.recordInsertion(
            text = "কাল সকাল ১০টায় দেখা হবে",
            language = Language.BENGALI,
            packageName = "com.whatsapp"
        )

        val items = DictationHistoryRepository.historyFlow.value
        assertEquals(1, items.size)
        assertEquals("কাল সকাল ১০টায় দেখা হবে", items[0].text)
        assertEquals(Language.BENGALI, items[0].language)
        assertEquals("com.whatsapp", items[0].packageName)
    }
}
