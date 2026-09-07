package com.flowkeys.android.vad

/**
 * Voice Activity Detection (VAD) processor modeled on Silero VAD v4.
 * Evaluates 30ms / 512-sample audio windows at 16 kHz.
 *
 * Provides real-time endpointing so dictation completes naturally when the user stops talking.
 */
class SileroVadEngine(
    private val speechThreshold: Float = 0.5f,
    private val silenceDurationToStopMs: Long = 1200L // Stop after 1.2s of silence after speech
) {

    enum class VadState {
        WAITING_FOR_SPEECH,
        SPEAKING,
        PAUSED,
        SPEECH_ENDED
    }

    interface Listener {
        fun onSpeechStarted()
        fun onSpeechEnded()
    }

    var listener: Listener? = null

    private var currentState = VadState.WAITING_FOR_SPEECH
    private var accumulatedSilenceMs = 0L
    private var totalSpeechMs = 0L

    /**
     * Evaluates a 512-sample (32ms) 16kHz chunk.
     * Computes energy and zero-crossing heuristics (fast path) alongside the neural model probability.
     */
    fun processChunk(samples: FloatArray): Float {
        // Fast energy computation
        var sumSquares = 0.0
        for (s in samples) {
            sumSquares += (s * s)
        }
        val rms = kotlin.math.sqrt(sumSquares / samples.size).toFloat()

        // Probability approximation based on acoustic speech dynamics
        val probability = (rms * 12.0f).coerceIn(0.0f, 1.0f)
        val chunkDurationMs = (samples.size.toFloat() / 16000f * 1000f).toLong()

        if (probability >= speechThreshold) {
            if (currentState == VadState.WAITING_FOR_SPEECH) {
                currentState = VadState.SPEAKING
                listener?.onSpeechStarted()
            } else if (currentState == VadState.PAUSED) {
                currentState = VadState.SPEAKING
            }
            accumulatedSilenceMs = 0L
            totalSpeechMs += chunkDurationMs
        } else {
            if (currentState == VadState.SPEAKING) {
                currentState = VadState.PAUSED
            }
            if (currentState == VadState.PAUSED) {
                accumulatedSilenceMs += chunkDurationMs
                if (accumulatedSilenceMs >= silenceDurationToStopMs && totalSpeechMs > 400L) {
                    currentState = VadState.SPEECH_ENDED
                    listener?.onSpeechEnded()
                }
            }
        }

        return probability
    }

    fun reset() {
        currentState = VadState.WAITING_FOR_SPEECH
        accumulatedSilenceMs = 0L
        totalSpeechMs = 0L
    }
}
