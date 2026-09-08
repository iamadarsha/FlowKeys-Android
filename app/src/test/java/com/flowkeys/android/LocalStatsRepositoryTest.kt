package com.flowkeys.android

import com.flowkeys.android.core.model.Language
import com.flowkeys.android.stats.LocalStatsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalStatsRepositoryTest {

    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_local_stats", ".json")
        LocalStatsRepository.initializeForTesting(tempFile)
    }

    @After
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testRecordSessionUpdatesWordCountsAndLanguages() = runBlocking {
        LocalStatsRepository.recordSession(
            text = "This is a five word sentence",
            language = Language.ENGLISH
        )
        kotlinx.coroutines.delay(150)

        var stats = LocalStatsRepository.stats.value
        assertEquals(6L, stats.totalWordsSpoken)
        assertEquals(1L, stats.totalSessions)
        assertEquals(6L, stats.englishWords)
        assertEquals(1L, stats.englishSessions)
        assertEquals(1, stats.currentStreakDays)

        LocalStatsRepository.recordSession(
            text = "আমি কলকাতা যাব",
            language = Language.BENGALI
        )
        kotlinx.coroutines.delay(150)

        stats = LocalStatsRepository.stats.value
        assertEquals(9L, stats.totalWordsSpoken)
        assertEquals(2L, stats.totalSessions)
        assertEquals(3L, stats.bengaliWords)
        assertEquals(1L, stats.bengaliSessions)
    }

    @Test
    fun testEstimatedTimeSavedCalculation() = runBlocking {
        // 140 words: typing = 140/35 = 4 mins, speaking = 140/140 = 1 min, saved = 3 mins
        val sampleWords = (1..140).joinToString(" ") { "word$it" }
        LocalStatsRepository.recordSession(sampleWords, Language.ENGLISH)
        kotlinx.coroutines.delay(150)

        val stats = LocalStatsRepository.stats.value
        assertEquals(140L, stats.totalWordsSpoken)
        assertEquals(3.0, stats.estimatedMinutesSaved, 0.01)
        assertEquals("3m", stats.formattedTimeSaved)
    }
}
