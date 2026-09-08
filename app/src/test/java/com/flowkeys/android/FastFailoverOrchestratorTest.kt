package com.flowkeys.android

import com.flowkeys.android.providers.FastFailoverOrchestrator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class FastFailoverOrchestratorTest {

    @Test
    fun testPrimaryFastSuccessPreventsSecondaryLaunch() = runBlocking {
        val secondaryLaunched = AtomicBoolean(false)

        val result = FastFailoverOrchestrator.race(
            hedgeDelayMs = 400L,
            primaryName = "Gemini",
            secondaryName = "Groq",
            primaryCall = {
                delay(100L)
                "Gemini Transcription"
            },
            secondaryCall = {
                secondaryLaunched.set(true)
                "Groq Transcription"
            }
        )

        assertEquals("Gemini Transcription", result)
        // Wait a bit to ensure hedge delay wouldn't trigger it
        delay(400L)
        assertTrue("Secondary should not be launched when primary succeeds before hedge delay", !secondaryLaunched.get())
    }

    @Test
    fun testPrimaryImmediateFailureTriggersSecondaryImmediately() = runBlocking {
        val startTime = System.currentTimeMillis()

        val result = FastFailoverOrchestrator.race(
            hedgeDelayMs = 1000L,
            primaryName = "Gemini",
            secondaryName = "Groq",
            primaryCall = {
                delay(50L)
                null // Error/failure
            },
            secondaryCall = {
                delay(100L)
                "Groq Failover Succeeded"
            }
        )

        val elapsed = System.currentTimeMillis() - startTime
        assertEquals("Groq Failover Succeeded", result)
        assertTrue("Secondary should complete well before 1000ms hedge delay: elapsed=$elapsed", elapsed < 800L)
    }

    @Test
    fun testPrimaryLagAllowsSecondaryToWin() = runBlocking {
        val result = FastFailoverOrchestrator.race(
            hedgeDelayMs = 150L,
            primaryName = "Gemini",
            secondaryName = "Groq",
            primaryCall = {
                delay(600L) // Slow Gemini
                "Gemini Slow"
            },
            secondaryCall = {
                delay(100L) // Fast Groq starts at 150ms and finishes at 250ms
                "Groq Fast"
            }
        )

        assertEquals("Groq Fast", result)
    }

    @Test
    fun testBothFailReturnsNull() = runBlocking {
        val result = FastFailoverOrchestrator.race<String>(
            hedgeDelayMs = 100L,
            primaryName = "Gemini",
            secondaryName = "Groq",
            primaryCall = {
                delay(50L)
                null
            },
            secondaryCall = {
                delay(50L)
                null
            }
        )

        assertNull(result)
    }
}
