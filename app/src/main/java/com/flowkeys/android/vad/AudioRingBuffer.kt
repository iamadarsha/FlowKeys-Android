package com.flowkeys.android.vad

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe circular audio buffer for transient speech capture.
 * Automatically bounds RAM usage to prevent Out-of-Memory crashes on 4 GB devices.
 */
class AudioRingBuffer(val maxSamples: Int = 16000 * 30) { // Default 30 seconds at 16kHz (~1.9 MB RAM)

    private val buffer = FloatArray(maxSamples)
    private var writePos = 0
    private var count = 0
    private val lock = ReentrantLock()

    fun write(samples: FloatArray) = lock.withLock {
        for (sample in samples) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % maxSamples
            if (count < maxSamples) {
                count++
            }
        }
    }

    /**
     * Extracts all recorded speech samples in chronological order.
     */
    fun toFloatArray(): FloatArray = lock.withLock {
        val result = FloatArray(count)
        val startPos = if (count < maxSamples) 0 else writePos
        for (i in 0 until count) {
            result[i] = buffer[(startPos + i) % maxSamples]
        }
        return result
    }

    fun clear() = lock.withLock {
        writePos = 0
        count = 0
    }

    val currentDurationSeconds: Float
        get() = lock.withLock { count.toFloat() / 16000f }
}
