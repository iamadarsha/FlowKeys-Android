package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.dictionary.LearnedVocabularyStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LearnedVocabularyStoreTest {

    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_learned_vocab", ".json")
        LearnedVocabularyStore.initializeForTesting(tempFile)
    }

    @After
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testRecordDictationExtractsProperNounsAndWords() = runBlocking {
        LearnedVocabularyStore.recordDictationWords(
            text = "Meeting with Subhashish at Temenos regarding the Upstox API",
            language = Language.ENGLISH
        )

        // Allow coroutine to complete writing
        kotlinx.coroutines.delay(200)

        val hints = LearnedVocabularyStore.getTopHints(Language.ENGLISH, limit = 10)
        assertTrue("Subhashish should be in hints", hints.contains("Subhashish"))
        assertTrue("Temenos should be in hints", hints.contains("Temenos"))
        assertTrue("Upstox should be in hints", hints.contains("Upstox"))
        assertFalse("Stop word 'with' should not be in hints", hints.contains("with"))
    }

    @Test
    fun testRecordCorrectionHasHighestPriority() = runBlocking {
        LearnedVocabularyStore.recordCorrection(
            spokenTerm = "Rhaul",
            correctedTerm = "Rahul",
            language = Language.ENGLISH
        )

        kotlinx.coroutines.delay(200)

        val hints = LearnedVocabularyStore.getTopHints(Language.ENGLISH, limit = 5)
        assertEquals("Rahul", hints.firstOrNull())
        assertEquals(1, LearnedVocabularyStore.getCorrectionCount())
    }

    @Test
    fun testReversibilityDeleteAndClear() = runBlocking {
        LearnedVocabularyStore.recordCorrection(
            spokenTerm = "timinus",
            correctedTerm = "Temenos",
            language = Language.ENGLISH
        )
        LearnedVocabularyStore.recordDictationWords("Visiting Kolkata today", Language.ENGLISH)
        kotlinx.coroutines.delay(200)

        assertTrue(LearnedVocabularyStore.getEntryCount() >= 2)

        LearnedVocabularyStore.deleteEntry("timinus")
        kotlinx.coroutines.delay(100)

        val hintsAfterDelete = LearnedVocabularyStore.getTopHints(Language.ENGLISH)
        assertFalse(hintsAfterDelete.contains("Temenos"))

        LearnedVocabularyStore.clearAll()
        kotlinx.coroutines.delay(100)
        assertEquals(0, LearnedVocabularyStore.getEntryCount())
    }

    @Test
    fun testDisabledLearningDoesNotRecord() = runBlocking {
        LearnedVocabularyStore.setEnabled(false)
        LearnedVocabularyStore.recordDictationWords("Visiting Bangalore tomorrow", Language.ENGLISH)
        kotlinx.coroutines.delay(100)

        assertEquals(0, LearnedVocabularyStore.getEntryCount())
        assertEquals(emptyList<String>(), LearnedVocabularyStore.getTopHints(Language.ENGLISH))
    }
}
