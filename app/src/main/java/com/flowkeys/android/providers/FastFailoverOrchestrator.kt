package com.flowkeys.android.providers

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-performance Hedged Provider Failover Orchestrator.
 *
 * Pattern:
 * 1. Start primary provider (e.g. Gemini 3.5 Flash Lite) immediately.
 * 2. If primary returns valid non-blank text within `hedgeDelayMs` (~800ms), return it directly.
 * 3. If primary produces an early error / null / exception, immediately trigger secondary (e.g. Groq Whisper) without waiting for hedge delay.
 * 4. If primary is still pending when `hedgeDelayMs` elapses, start secondary concurrently.
 * 5. Whichever returns a valid non-blank result first wins; cancel the sibling request immediately.
 * 6. If both fail, return null to allow caller fallback (local/rule-based).
 */
object FastFailoverOrchestrator {

    private const val TAG = "FastFailover"
    const val DEFAULT_HEDGE_DELAY_MS = 800L

    suspend fun <T : Any> race(
        hedgeDelayMs: Long = DEFAULT_HEDGE_DELAY_MS,
        primaryName: String = "Gemini",
        secondaryName: String = "Groq",
        primaryCall: suspend () -> T?,
        secondaryCall: (suspend () -> T?)? = null,
        validator: (T) -> Boolean = { true }
    ): T? = withContext(Dispatchers.IO) {
        if (secondaryCall == null) {
            return@withContext try {
                val res = primaryCall()
                if (res != null && validator(res)) res else null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Primary ($primaryName) failed: ${e.message}")
                null
            }
        }

        val resultDeferred = CompletableDeferred<T>()
        var primaryJob: Job? = null
        var secondaryJob: Job? = null
        var secondaryStarted = false
        val secondaryLock = Any()

        fun startSecondary(scope: CoroutineScope, reason: String) {
            synchronized(secondaryLock) {
                if (secondaryStarted || resultDeferred.isCompleted) return
                secondaryStarted = true
            }
            Log.d(TAG, "Launching $secondaryName ($reason)")
            secondaryJob = scope.launch {
                try {
                    val secRes = secondaryCall()
                    if (secRes != null && validator(secRes)) {
                        if (resultDeferred.complete(secRes)) {
                            Log.d(TAG, "Winner: $secondaryName")
                            primaryJob?.cancel()
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.w(TAG, "Secondary ($secondaryName) error: ${e.message}")
                    }
                }
            }
        }

        primaryJob = launch {
            try {
                val primRes = primaryCall()
                if (primRes != null && validator(primRes)) {
                    if (resultDeferred.complete(primRes)) {
                        Log.d(TAG, "Winner: $primaryName")
                        secondaryJob?.cancel()
                    }
                } else {
                    // Primary completed but returned null/invalid: trigger secondary immediately
                    startSecondary(this@withContext, "Primary returned null/invalid")
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.w(TAG, "Primary ($primaryName) error: ${e.message}")
                    startSecondary(this@withContext, "Primary threw exception")
                }
            }
        }

        // Hedging timer job
        val hedgeJob = launch {
            delay(hedgeDelayMs)
            if (!resultDeferred.isCompleted) {
                startSecondary(this@withContext, "Hedge delay (${hedgeDelayMs}ms) expired")
            }
        }

        // Wait for primary, secondary, or completion
        val combinedWatcher = launch {
            primaryJob.join()
            hedgeJob.cancel()
            // If primary finished with failure and secondary was started, wait for secondary
            secondaryJob?.join()
            // If neither completed, mark complete with exception/null
            if (!resultDeferred.isCompleted) {
                resultDeferred.cancel()
            }
        }

        val finalResult = try {
            resultDeferred.await()
        } catch (e: Exception) {
            null
        } finally {
            primaryJob.cancel()
            secondaryJob?.cancel()
            hedgeJob.cancel()
            combinedWatcher.cancel()
        }

        return@withContext finalResult
    }
}
